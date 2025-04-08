package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.*;
import com.ercanbeyen.bankingapplication.constant.message.LogMessage;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.constant.query.SummaryField;
import com.ercanbeyen.bankingapplication.dto.request.AccountActivityRequest;
import com.ercanbeyen.bankingapplication.dto.request.MoneyExchangeRequest;
import com.ercanbeyen.bankingapplication.dto.request.MoneyTransferRequest;
import com.ercanbeyen.bankingapplication.entity.Account;
import com.ercanbeyen.bankingapplication.entity.AccountActivity;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import com.ercanbeyen.bankingapplication.exception.ResourceExpectationFailedException;
import com.ercanbeyen.bankingapplication.repository.AccountRepository;
import com.ercanbeyen.bankingapplication.service.*;
import com.ercanbeyen.bankingapplication.util.AccountUtil;
import com.ercanbeyen.bankingapplication.util.FormatterUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.DoublePredicate;

@RequiredArgsConstructor
@Slf4j
@Service
@Transactional
public class TransactionService {
    private final AccountRepository accountRepository;
    private final AccountActivityService accountActivityService;
    private final ExchangeService exchangeService;
    private final ChargeService chargeService;
    private final FeeService feeService;
    private final CashFlowCalendarService cashFlowCalendarService;

    public void applyAccountActivityForSingleAccount(AccountActivityType activityType, Double amount, Account account, String cashFlowExplanation) {
        Account[] accounts = new Account[2]; // first account is sender, second account is receiver
        Double transactionFee = getTransactionFee(activityType, List.of(account));
        log.info(LogMessage.ACCOUNT_ACTIVITY_STATUS_ECHO, activityType, amount, transactionFee);

        final double previousBalance = account.getBalance();
        DoublePredicate validBalancePredicate = balance -> balance >= 0;

        double newBalance = switch (activityType) {
            case MONEY_DEPOSIT, FEE -> {
                double updatedBalance = previousBalance + amount - transactionFee;

                if (!validBalancePredicate.test(updatedBalance)) {
                    throw new ResourceExpectationFailedException(ResponseMessage.TRANSACTION_FEE_CANNOT_BE_PAYED);
                }

                /* Balance update of receiver account */
                accounts[1] = account;
                yield updatedBalance;
            }
            case WITHDRAWAL -> {
                double updatedBalance = previousBalance - (amount + transactionFee);

                if (!validBalancePredicate.test(updatedBalance)) {
                    throw new ResourceExpectationFailedException(ResponseMessage.INSUFFICIENT_FUNDS);
                }

                /* Balance update of sender account */
                accounts[0] = account;
                yield updatedBalance;
            }
            default -> throw new ResourceConflictException(ResponseMessage.IMPROPER_ACCOUNT_ACTIVITY);
        };

        log.info(LogMessage.ENOUGH_BALANCE, activityType);

        updateBalance(account, newBalance);
        accountRepository.saveAndFlush(account);

        String amountInSummary = FormatterUtil.convertNumberToFormalExpression(amount);

        Map<String, Object> summary = new HashMap<>();
        summary.put(SummaryField.ACCOUNT_ACTIVITY, activityType.getValue());
        summary.put(SummaryField.FULL_NAME, account.getCustomer().getFullName());
        summary.put(SummaryField.NATIONAL_IDENTITY, account.getCustomer().getNationalId());
        summary.put(SummaryField.ACCOUNT_IDENTITY, account.getId());
        summary.put(SummaryField.AMOUNT, amountInSummary + " " + account.getCurrency());
        summary.put(SummaryField.TRANSACTION_FEE, transactionFee);
        summary.put(SummaryField.TIME, LocalDateTime.now().toString());

        AccountActivity accountActivity = createAccountActivity(activityType, amount, summary, accounts, null);
        createAccountActivityForCharge(transactionFee, summary, account);

        cashFlowCalendarService.createCashFlow(account.getCustomer().getCashFlowCalendar(), accountActivity, cashFlowExplanation);
    }

    public void transferMoneyBetweenAccounts(MoneyTransferRequest request, Double amount, Account senderAccount, Account receiverAccount, Account chargedAccount) {
        AccountActivityType activityType = AccountActivityType.MONEY_TRANSFER;
        List<Account> accountsInMoneyTransfer = List.of(senderAccount, receiverAccount);
        double transactionFee = getTransactionFee(activityType, accountsInMoneyTransfer);
        checkBalanceBeforeMoneyTransferAndExchange(chargedAccount, accountsInMoneyTransfer, amount, transactionFee, activityType);

        /* Balance update of sender account */
        double newBalance = senderAccount.getBalance() - amount;
        updateBalance(senderAccount, newBalance);

        /* Balance update of charged account */
        newBalance = chargedAccount.getBalance() - transactionFee;
        updateBalance(chargedAccount, newBalance);

        /* Balance update of receiver account */
        newBalance = receiverAccount.getBalance() + amount;
        updateBalance(receiverAccount, newBalance);

        accountRepository.saveAllAndFlush(List.of(senderAccount, chargedAccount, receiverAccount));

        Account[] accounts = {senderAccount, receiverAccount};
        String amountInSummary = FormatterUtil.convertNumberToFormalExpression(amount);

        Map<String, Object> summary = new HashMap<>();
        summary.put(Entity.ACCOUNT_ACTIVITY.getValue(), activityType.getValue());
        summary.put(SummaryField.FULL_NAME, senderAccount.getCustomer().getFullName());
        summary.put(SummaryField.NATIONAL_IDENTITY, senderAccount.getCustomer().getNationalId());
        summary.put("Sender " + SummaryField.ACCOUNT_IDENTITY, senderAccount.getId());
        summary.put("Receiver " + SummaryField.ACCOUNT_IDENTITY, receiverAccount.getId());
        putChargedAccountInformationIntoSummary(senderAccount, chargedAccount, summary);
        summary.put(SummaryField.AMOUNT, amountInSummary + " " + senderAccount.getCurrency());
        summary.put(SummaryField.TRANSACTION_FEE, transactionFee + " " + Currency.getChargeCurrency());
        summary.put(SummaryField.PAYMENT_TYPE, request.paymentType());
        summary.put(SummaryField.TIME, LocalDateTime.now().toString());

        AccountActivity accountActivity = createAccountActivity(activityType, request.amount(), summary, accounts, request.explanation());
        createAccountActivityForCharge(transactionFee, summary, chargedAccount);

        if (!senderAccount.getCustomer().getNationalId().equals(receiverAccount.getCustomer().getNationalId())) {
            String entity = Entity.ACCOUNT.getValue();
            String explanation = entity + " " + senderAccount.getId() + " sent " + amount + " " + senderAccount.getCurrency();
            cashFlowCalendarService.createCashFlow(senderAccount.getCustomer().getCashFlowCalendar(), accountActivity, explanation);
            explanation = entity + " " + receiverAccount.getId() + " received " + amount + receiverAccount.getCurrency();
            cashFlowCalendarService.createCashFlow(receiverAccount.getCustomer().getCashFlowCalendar(), accountActivity, explanation);
        }
    }

    public void exchangeMoneyBetweenAccounts(MoneyExchangeRequest request, Account sellerAccount, Account buyerAccount, Account chargedAccount) {
        AccountActivityType activityType = AccountActivityType.MONEY_EXCHANGE;

        List<Account> accountsInMoneyExchange = List.of(sellerAccount, buyerAccount);
        double transactionFee = getTransactionFee(activityType, accountsInMoneyExchange);
        checkBalanceBeforeMoneyTransferAndExchange(chargedAccount, accountsInMoneyExchange, request.amount(), transactionFee, activityType);

        Double rate = exchangeService.getBankExchangeRate(sellerAccount.getCurrency(), buyerAccount.getCurrency());
        Double spentAmount = request.amount();
        Double earnedAmount = exchangeService.convertMoneyBetweenCurrencies(sellerAccount.getCurrency(), buyerAccount.getCurrency(), spentAmount);

        /* Balance update of seller account */
        double newBalance = sellerAccount.getBalance() - spentAmount;
        updateBalance(sellerAccount, newBalance);

        /* Balance update of charged account */
        newBalance = chargedAccount.getBalance() - transactionFee;
        updateBalance(chargedAccount, newBalance);

        /* Balance update of receiver account */
        newBalance = buyerAccount.getBalance() + earnedAmount;
        updateBalance(buyerAccount, newBalance);

        accountRepository.saveAllAndFlush(List.of(sellerAccount, chargedAccount, buyerAccount));

        String spentAmountInSummary = FormatterUtil.convertNumberToFormalExpression(spentAmount);
        log.info(LogMessage.PROCESSED_AMOUNT, spentAmountInSummary, "Spent");

        String earnedAmountInSummary = FormatterUtil.convertNumberToFormalExpression(earnedAmount);
        log.info(LogMessage.PROCESSED_AMOUNT, earnedAmountInSummary, "Earn");

        Account[] accounts = {sellerAccount, buyerAccount};

        Map<String, Object> summary = new HashMap<>();
        summary.put(Entity.ACCOUNT_ACTIVITY.getValue(), activityType.getValue());
        summary.put(SummaryField.FULL_NAME, sellerAccount.getCustomer().getFullName());
        summary.put(SummaryField.NATIONAL_IDENTITY, sellerAccount.getCustomer().getNationalId());
        summary.put("Seller " + SummaryField.ACCOUNT_IDENTITY, sellerAccount.getId());
        summary.put("Buyer " + SummaryField.ACCOUNT_IDENTITY, buyerAccount.getId());
        putChargedAccountInformationIntoSummary(sellerAccount, chargedAccount, summary);
        summary.put("Spent " + SummaryField.AMOUNT, spentAmountInSummary + " " + sellerAccount.getCurrency());
        summary.put("Earned " + SummaryField.AMOUNT, earnedAmountInSummary + " " + buyerAccount.getCurrency());
        summary.put(SummaryField.RATE, rate);
        summary.put(SummaryField.TRANSACTION_FEE, transactionFee + " " + Currency.getChargeCurrency());
        summary.put(SummaryField.TIME, LocalDateTime.now().toString());

        createAccountActivity(activityType, earnedAmount, summary, accounts, null);
        createAccountActivityForCharge(transactionFee, summary, chargedAccount);
    }

    private void updateBalance(Account account, double newBalance) {
        if (account.getType() == AccountType.DEPOSIT) {
            log.info("{} {} needs to update its interest ratio, before balance update", AccountType.DEPOSIT.getValue(), Entity.ACCOUNT.getValue());
            double interestRatio = feeService.getInterestRatio(account.getCurrency(), account.getDepositPeriod(), newBalance);
            double balanceAfterNextFee = AccountUtil.calculateBalanceAfterNextFee(newBalance, account.getDepositPeriod(), interestRatio);

            account.setInterestRatio(interestRatio);
            account.setBalanceAfterNextFee(balanceAfterNextFee);
        }

        account.setBalance(newBalance);
        log.info("Account balance is updated");
    }

    private double getTransactionFee(AccountActivityType activityType, List<Account> accounts) {
        boolean sameCustomerTransferMoneyBetweenAccounts = activityType == AccountActivityType.MONEY_TRANSFER
                && accounts.getFirst().getCustomer().getNationalId().equals(accounts.getLast().getCustomer().getNationalId());

        if (sameCustomerTransferMoneyBetweenAccounts) {
            log.warn("There is no transaction fee when transferring money between accounts of the same customer");
            return 0;
        }

        return chargeService.getCharge(activityType).amount();
    }

    private void createAccountActivityForCharge(Double transactionFee, Map<String, Object> summary, Account chargedAccount) {
        if (transactionFee == 0) {
            log.warn("There is no transaction fee");
            return;
        }

        Account[] accounts = new Account[2];
        accounts[0] = chargedAccount;

        createAccountActivity(AccountActivityType.CHARGE, transactionFee, summary, accounts, null);
    }

    private AccountActivity createAccountActivity(AccountActivityType activityType, Double amount, Map<String, Object> summary, Account[] accounts, String explanation) {
        AccountActivityRequest accountActivityRequest = new AccountActivityRequest(activityType, accounts[0], accounts[1], amount, summary, explanation);
        return accountActivityService.createAccountActivity(accountActivityRequest);
    }

    private void checkBalanceBeforeMoneyTransferAndExchange(Account chargedAccount, List<Account> relatedAccounts, Double amount, Double transactionFee, AccountActivityType activityType) {
        log.info(LogMessage.ACCOUNT_ACTIVITY_STATUS_ECHO, activityType, amount, transactionFee);

        if (Objects.equals(chargedAccount.getId(), relatedAccounts.getFirst().getId())) {
            log.info("Extra charged account does not exist");

            if (chargedAccount.getBalance() < (amount + transactionFee)) {
                throw new ResourceExpectationFailedException(ResponseMessage.INSUFFICIENT_FUNDS);
            }
        } else {
            log.info("Extra charged account exists");

            if (chargedAccount.getBalance() < transactionFee) {
                throw new ResourceExpectationFailedException(ResponseMessage.TRANSACTION_FEE_CANNOT_BE_PAYED);
            }

            if (relatedAccounts.getFirst().getBalance() < amount) {
                throw new ResourceExpectationFailedException(ResponseMessage.INSUFFICIENT_FUNDS);
            }
        }

        log.info(LogMessage.ENOUGH_BALANCE, activityType);
    }

    private static void putChargedAccountInformationIntoSummary(Account relatedAccount, Account chargedAccount, Map<String, Object> summary) {
        if (!chargedAccount.getId().equals(relatedAccount.getId())) {
            log.info("There is a separate charged account, so add it into summary");
            summary.put("Charged " + SummaryField.ACCOUNT_ID, chargedAccount.getId());
        } else {
            log.warn("Charge and related accounts are same, so no need to add it into summary");
        }
    }
}
