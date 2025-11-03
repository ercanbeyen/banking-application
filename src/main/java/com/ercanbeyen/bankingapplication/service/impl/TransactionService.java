package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.*;
import com.ercanbeyen.bankingapplication.constant.message.LogMessage;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.constant.query.SummaryField;
import com.ercanbeyen.bankingapplication.dto.FeeDto;
import com.ercanbeyen.bankingapplication.dto.request.AccountActivityRequest;
import com.ercanbeyen.bankingapplication.dto.request.MoneyExchangeRequest;
import com.ercanbeyen.bankingapplication.dto.request.MoneyTransferRequest;
import com.ercanbeyen.bankingapplication.entity.Account;
import com.ercanbeyen.bankingapplication.entity.AccountActivity;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import com.ercanbeyen.bankingapplication.exception.ResourceExpectationFailedException;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.option.FeeFilteringOption;
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
        Account[] accounts = new Account[2]; // first account is sender, second account is recipient
        Double transactionFee = getTransactionFee(activityType, List.of(account));
        log.info(LogMessage.ACCOUNT_ACTIVITY_STATUS_ECHO, activityType.getValue(), amount, transactionFee);

        final double previousBalance = account.getBalance();
        DoublePredicate validBalancePredicate = balance -> balance >= 0;

        double newBalance = switch (activityType) {
            case MONEY_DEPOSIT, FEE -> {
                double updatedBalance = previousBalance + amount - transactionFee;

                if (!validBalancePredicate.test(updatedBalance)) {
                    throw new ResourceExpectationFailedException(ResponseMessage.TRANSACTION_FEE_CANNOT_BE_PAYED);
                }

                /* Balance update of recipient account */
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

        log.info(LogMessage.ENOUGH_BALANCE, activityType.getValue());

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

    public void transferMoneyBetweenAccounts(MoneyTransferRequest request, Double amount, Account senderAccount, Account recipientAccount, Account chargedAccount) {
        AccountActivityType activityType = AccountActivityType.MONEY_TRANSFER;
        List<Account> accountsInMoneyTransfer = List.of(senderAccount, recipientAccount);
        double transactionFee = getTransactionFee(activityType, accountsInMoneyTransfer);
        checkBalanceBeforeMoneyTransferAndExchange(chargedAccount, accountsInMoneyTransfer, amount, transactionFee, activityType);

        /* Balance update of sender account */
        double newBalance = senderAccount.getBalance() - amount;
        updateBalance(senderAccount, newBalance);

        /* Balance update of charged account */
        newBalance = chargedAccount.getBalance() - transactionFee;
        updateBalance(chargedAccount, newBalance);

        /* Balance update of recipient account */
        newBalance = recipientAccount.getBalance() + amount;
        updateBalance(recipientAccount, newBalance);

        accountRepository.saveAllAndFlush(List.of(senderAccount, chargedAccount, recipientAccount));

        Account[] accounts = {senderAccount, recipientAccount};
        String amountInSummary = FormatterUtil.convertNumberToFormalExpression(amount);

        Map<String, Object> summary = new HashMap<>();
        summary.put(Entity.ACCOUNT_ACTIVITY.getValue(), activityType.getValue());
        summary.put(SummaryField.FULL_NAME, senderAccount.getCustomer().getFullName());
        summary.put(SummaryField.NATIONAL_IDENTITY, senderAccount.getCustomer().getNationalId());
        summary.put("Sender " + SummaryField.ACCOUNT_IDENTITY, senderAccount.getId());
        summary.put("Recipient " + SummaryField.ACCOUNT_IDENTITY, recipientAccount.getId());
        putChargedAccountInformationIntoSummary(senderAccount, chargedAccount, summary);
        summary.put(SummaryField.AMOUNT, amountInSummary + " " + senderAccount.getCurrency());
        summary.put(SummaryField.TRANSACTION_FEE, transactionFee + " " + Currency.getChargeCurrency());
        summary.put(SummaryField.PAYMENT_TYPE, request.paymentType());
        summary.put(SummaryField.TIME, LocalDateTime.now().toString());

        AccountActivity accountActivity = createAccountActivity(activityType, request.amount(), summary, accounts, request.explanation());
        createAccountActivityForCharge(transactionFee, summary, chargedAccount);

        if (!senderAccount.getCustomer().getNationalId().equals(recipientAccount.getCustomer().getNationalId())) {
            String entity = Entity.ACCOUNT.getValue();
            String explanation = entity + " " + senderAccount.getId() + " sent " + amount + " " + senderAccount.getCurrency();
            cashFlowCalendarService.createCashFlow(senderAccount.getCustomer().getCashFlowCalendar(), accountActivity, explanation);
            explanation = entity + " " + recipientAccount.getId() + " received " + amount + recipientAccount.getCurrency();
            cashFlowCalendarService.createCashFlow(recipientAccount.getCustomer().getCashFlowCalendar(), accountActivity, explanation);
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

        /* Balance update of recipient account */
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

    public void updateDepositAccountFields(Account account, double balance, int depositPeriod) {
        double interestRatio = getInterestRatio(account.getCurrency(), balance, depositPeriod);
        double balanceAfterNextFee = AccountUtil.calculateBalanceAfterNextFee(balance, depositPeriod, interestRatio);

        account.setInterestRatio(interestRatio);
        account.setDepositPeriod(depositPeriod);
        account.setBalanceAfterNextFee(balanceAfterNextFee);

        log.info("{} {} related fields are updated", AccountType.DEPOSIT.getValue(), Entity.ACCOUNT.getValue());
    }

    private void updateBalance(Account account, double balance) {
        if (AccountUtil.checkAccountTypeMatch.test(account.getType(), AccountType.DEPOSIT)) {
            log.info(LogMessage.DEPOSIT_ACCOUNT_FIELDS_SHOULD_UPDATE);
            updateDepositAccountFields(account, balance, account.getDepositPeriod());
        }

        account.setBalance(balance);
        log.info("Account balance is updated");
    }

    private double getInterestRatio(Currency currency, double balance, int depositPeriod) {
        double interestRatio = 0;

        /* Match interest ratio for the given currency, balance and deposit period */
        try {
            interestRatio = feeService.getInterestRatio(currency, depositPeriod, balance);
        } catch (ResourceNotFoundException exception) {
            FeeFilteringOption filteringOption = new FeeFilteringOption();
            filteringOption.setCurrency(currency);
            filteringOption.setDepositPeriod(depositPeriod);

            String entity = Entity.FEE.getValue();
            String exceptionMessage = "No %s amount in the " + entity.toLowerCase();

            /* Less than Minimum Fee Amount */
            double minimumAmount = feeService.getEntities(filteringOption)
                    .stream()
                    .mapToDouble(FeeDto::getMinimumAmount)
                    .min()
                    .orElseThrow(() -> new ResourceNotFoundException(String.format(exceptionMessage, "minimum")));

            if (balance < minimumAmount) {
                log.info("Balance is less than the minimum {} amount for deposit period {}. Therefore, interest ratio is {}", entity.toLowerCase(), depositPeriod, interestRatio);
                return interestRatio;
            }

            /* Greater than Maximum Fee Amount */
            double maximumAmount = feeService.getEntities(filteringOption)
                    .stream()
                    .mapToDouble(FeeDto::getMaximumAmount)
                    .max()
                    .orElseThrow(() -> new ResourceNotFoundException(String.format(exceptionMessage, "maximum")));

            if (balance > maximumAmount) {
                log.info("Balance is greater than or equal to the maximum amount for deposit period {}. Therefore, interest ratio of the maximum {} amount", depositPeriod, entity.toLowerCase());
                return feeService.getInterestRatio(currency, depositPeriod, maximumAmount);
            }

            /* Unmatched Fee Amount Case */
            log.error("Unexpected condition! There are unmatched {} amounts. Amount: {} & Deposit period {}", entity.toLowerCase(), balance, depositPeriod);
            throw new ResourceNotFoundException(entity + " amount is not found for " + balance);
        }

        return interestRatio;
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
        log.info(LogMessage.ACCOUNT_ACTIVITY_STATUS_ECHO, activityType.getValue(), amount, transactionFee);
        String entity = Entity.ACCOUNT.getValue().toLowerCase();

        if (Objects.equals(chargedAccount.getId(), relatedAccounts.getFirst().getId())) {
            log.info("Extra charged {} does not exist", entity);

            if (chargedAccount.getBalance() < (amount + transactionFee)) {
                throw new ResourceExpectationFailedException(ResponseMessage.INSUFFICIENT_FUNDS);
            }
        } else {
            log.info("Extra charged {} exists", entity);

            if (chargedAccount.getBalance() < transactionFee) {
                throw new ResourceExpectationFailedException(ResponseMessage.TRANSACTION_FEE_CANNOT_BE_PAYED);
            }

            if (relatedAccounts.getFirst().getBalance() < amount) {
                throw new ResourceExpectationFailedException(ResponseMessage.INSUFFICIENT_FUNDS);
            }
        }

        log.info(LogMessage.ENOUGH_BALANCE, activityType.getValue());
    }

    private static void putChargedAccountInformationIntoSummary(Account relatedAccount, Account chargedAccount, Map<String, Object> summary) {
        String entity = Entity.ACCOUNT.getValue().toLowerCase();

        if (chargedAccount.getId().equals(relatedAccount.getId())) {
            log.warn("Charge and related {}s are same, so no need to add it into summary", entity);
            return;
        }

        log.info("There is a separate charged {}, so add it into summary", entity);
        summary.put("Charged " + SummaryField.ACCOUNT_ID, chargedAccount.getId());
    }
}
