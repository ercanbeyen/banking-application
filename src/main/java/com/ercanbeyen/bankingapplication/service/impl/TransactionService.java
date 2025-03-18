package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.*;
import com.ercanbeyen.bankingapplication.constant.message.LogMessage;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.constant.query.SummaryFields;
import com.ercanbeyen.bankingapplication.dto.request.AccountActivityRequest;
import com.ercanbeyen.bankingapplication.dto.request.MoneyExchangeRequest;
import com.ercanbeyen.bankingapplication.dto.request.MoneyTransferRequest;
import com.ercanbeyen.bankingapplication.entity.Account;
import com.ercanbeyen.bankingapplication.entity.AccountActivity;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import com.ercanbeyen.bankingapplication.exception.ResourceExpectationFailedException;
import com.ercanbeyen.bankingapplication.repository.AccountRepository;
import com.ercanbeyen.bankingapplication.service.AccountActivityService;
import com.ercanbeyen.bankingapplication.service.CashFlowCalendarService;
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

@RequiredArgsConstructor
@Slf4j
@Service
@Transactional
public class TransactionService {
    private final AccountRepository accountRepository;
    private final AccountActivityService accountActivityService;
    private final ExchangeService exchangeService;
    private final ChargeServiceImpl chargeService;
    private final FeeService feeService;
    private final CashFlowCalendarService cashFlowCalendarService;

    public void updateBalanceOfSingleAccount(AccountActivityType activityType, Double amount, Account account, String cashFlowExplanation) {
        Double transactionFee = getTransactionFee(activityType, List.of(account));

        Account[] accounts = new Account[2]; // first account is sender, second account is receiver
        final double previousBalance = account.getBalance();

        log.info(LogMessage.ACCOUNT_ACTIVITY_STATUS_ECHO, activityType, amount, transactionFee);

        double newBalance = switch (activityType) {
            case MONEY_DEPOSIT, FEE -> {
                if (previousBalance + amount < transactionFee) {
                    throw new ResourceExpectationFailedException(ResponseMessage.TRANSACTION_FEE_CANNOT_BE_PAYED);
                }

                /* Balance update of receiver account */
                accounts[1] = account;
                yield previousBalance + amount - transactionFee;
            }
            case WITHDRAWAL -> {
                if (previousBalance < amount + transactionFee) {
                    throw new ResourceExpectationFailedException(ResponseMessage.INSUFFICIENT_FUNDS);
                }

                /* Balance update of sender account */
                accounts[0] = account;
                yield previousBalance - (amount + transactionFee);
            }
            default -> throw new ResourceConflictException(ResponseMessage.IMPROPER_ACCOUNT_ACTIVITY);
        };

        account.setBalance(newBalance);

        String entity = Entity.ACCOUNT.getValue();

        if (account.getType() == AccountType.DEPOSIT) {
            log.info("{} {} is needs to update its interest ratio, before balance update", AccountType.DEPOSIT.getValue(), entity);
            double interestRatio = feeService.getInterestRatio(account.getCurrency(), account.getDepositPeriod(), newBalance);
            double balanceAfterNextFee = AccountUtil.calculateBalanceAfterNextFee(newBalance, account.getDepositPeriod(), interestRatio);

            account.setInterestRatio(interestRatio);
            account.setBalanceAfterNextFee(balanceAfterNextFee);
        }

        accountRepository.saveAndFlush(account);

        String amountInSummary = FormatterUtil.convertNumberToFormalExpression(amount);

        Map<String, Object> summary = new HashMap<>();
        summary.put(SummaryFields.ACCOUNT_ACTIVITY, activityType.getValue());
        summary.put(SummaryFields.FULL_NAME, account.getCustomer().getFullName());
        summary.put(SummaryFields.NATIONAL_IDENTITY, account.getCustomer().getNationalId());
        summary.put(SummaryFields.ACCOUNT_IDENTITY, account.getId());
        summary.put(SummaryFields.AMOUNT, amountInSummary + " " + account.getCurrency());
        summary.put(SummaryFields.TRANSACTION_FEE, transactionFee);
        summary.put(SummaryFields.TIME, LocalDateTime.now().toString());

        AccountActivity accountActivity = createAccountActivity(activityType, amount, summary, accounts, null);
        createAccountActivityForCharge(transactionFee, summary, accounts);

        cashFlowCalendarService.createCashFlow(account.getCustomer().getCashFlowCalendar(), accountActivity, cashFlowExplanation);
    }

    public void transferMoneyBetweenAccounts(MoneyTransferRequest request, Double amount, Account senderAccount, Account receiverAccount, Account chargedAccount) {
        AccountActivityType activityType = AccountActivityType.MONEY_TRANSFER;
        List<Account> accountsInMoneyTransfer = List.of(senderAccount, receiverAccount);
        double transactionFee = getTransactionFee(activityType, accountsInMoneyTransfer);
        checkBalanceBeforeMoneyTransferAndExchange(chargedAccount, accountsInMoneyTransfer, amount, transactionFee, activityType);

        /* Balance update of sender account */
        double newBalance = senderAccount.getBalance() - amount;
        senderAccount.setBalance(newBalance);

        /* Balance update of charged account */
        newBalance = chargedAccount.getBalance() - transactionFee;
        chargedAccount.setBalance(newBalance);

        /* Balance update of receiver account */
        newBalance = receiverAccount.getBalance() + amount;
        receiverAccount.setBalance(newBalance);

        accountRepository.saveAllAndFlush(List.of(senderAccount, chargedAccount, receiverAccount));

        Account[] accounts = {senderAccount, receiverAccount};
        String amountInSummary = FormatterUtil.convertNumberToFormalExpression(amount);

        Map<String, Object> summary = new HashMap<>();
        summary.put(Entity.ACCOUNT_ACTIVITY.getValue(), activityType.getValue());
        summary.put(SummaryFields.FULL_NAME, senderAccount.getCustomer().getFullName());
        summary.put(SummaryFields.NATIONAL_IDENTITY, senderAccount.getCustomer().getNationalId());
        summary.put("Sender " + SummaryFields.ACCOUNT_IDENTITY, senderAccount.getId());
        summary.put("Receiver " + SummaryFields.ACCOUNT_IDENTITY, receiverAccount.getId());
        summary.put(SummaryFields.AMOUNT, amountInSummary + " " + senderAccount.getCurrency());
        summary.put(SummaryFields.TRANSACTION_FEE, transactionFee + " " + Currency.getChargeCurrency());
        summary.put(SummaryFields.PAYMENT_TYPE, request.paymentType());
        summary.put(SummaryFields.TIME, LocalDateTime.now().toString());

        AccountActivity accountActivity = createAccountActivity(activityType, request.amount(), summary, accounts, request.explanation());
        createAccountActivityForCharge(transactionFee, summary, accounts);

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
        sellerAccount.setBalance(newBalance);

        /* Balance update of charged account */
        newBalance = chargedAccount.getBalance() - transactionFee;
        chargedAccount.setBalance(newBalance);

        /* Balance update of receiver account */
        newBalance = buyerAccount.getBalance() + earnedAmount;
        buyerAccount.setBalance(newBalance);

        accountRepository.saveAllAndFlush(List.of(sellerAccount, chargedAccount, buyerAccount));

        String spentAmountInSummary = FormatterUtil.convertNumberToFormalExpression(spentAmount);
        log.info(LogMessage.PROCESSED_AMOUNT, spentAmountInSummary, "Spent");

        String earnedAmountInSummary = FormatterUtil.convertNumberToFormalExpression(earnedAmount);
        log.info(LogMessage.PROCESSED_AMOUNT, earnedAmountInSummary, "Earn");

        Account[] accounts = {sellerAccount, buyerAccount};

        Map<String, Object> summary = new HashMap<>();
        summary.put(Entity.ACCOUNT_ACTIVITY.getValue(), activityType.getValue());
        summary.put(SummaryFields.FULL_NAME, sellerAccount.getCustomer().getFullName());
        summary.put(SummaryFields.NATIONAL_IDENTITY, sellerAccount.getCustomer().getNationalId());
        summary.put("Seller " + SummaryFields.ACCOUNT_IDENTITY, sellerAccount.getId());
        summary.put("Buyer " + SummaryFields.ACCOUNT_IDENTITY, buyerAccount.getId());
        summary.put("Spent " + SummaryFields.AMOUNT, spentAmountInSummary + " " + sellerAccount.getCurrency());
        summary.put("Earned " + SummaryFields.AMOUNT, earnedAmountInSummary + " " + buyerAccount.getCurrency());
        summary.put(SummaryFields.RATE, rate);
        summary.put(SummaryFields.TRANSACTION_FEE, transactionFee + " " + Currency.getChargeCurrency());
        summary.put(SummaryFields.TIME, LocalDateTime.now().toString());

        createAccountActivity(activityType, earnedAmount, summary, accounts, null);
        createAccountActivityForCharge(transactionFee, summary, accounts);
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

    private void createAccountActivityForCharge(Double transactionFee, Map<String, Object> summary, Account[] accounts) {
        if (transactionFee == 0) {
            log.warn("There is no transaction fee");
            return;
        }

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
}
