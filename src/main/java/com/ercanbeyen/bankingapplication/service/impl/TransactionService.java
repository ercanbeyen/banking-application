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

    public void updateBalanceOfSingleAccount(AccountActivityType activityType, Double amount, Account account) {
        Account[] accounts = new Account[2]; // first account is sender, second account is receiver
        double newBalance;

        switch (activityType) {
            case MONEY_DEPOSIT, FEE ->  {
                newBalance = account.getBalance() + amount;
                accounts[1] = account; // receiver
            }
            case WITHDRAWAL, CHARGE -> {
                newBalance = account.getBalance() - amount;
                accounts[0] = account; // sender
            }
            default -> throw new ResourceConflictException(ResponseMessage.IMPROPER_ACCOUNT_ACTIVITY);
        }

        account.setBalance(newBalance);

        if (account.getType() == AccountType.DEPOSIT) {
            log.info("{} {} is needs to update its interest ratio, before balance update", AccountType.DEPOSIT.getValue(), Entity.ACCOUNT.getValue());
            double interestRatio = feeService.getInterestRatio(account.getCurrency(), account.getDepositPeriod(), newBalance);
            double balanceAfterNextFee = AccountUtil.calculateBalanceAfterNextFee(newBalance, account.getDepositPeriod(), interestRatio);

            account.setInterestRatio(interestRatio);
            account.setBalanceAfterNextFee(balanceAfterNextFee);
        }

        accountRepository.saveAndFlush(account);

        Double transactionFee = getTransactionFee(activityType, List.of(account));
        String requestedAmountInSummary = FormatterUtil.convertNumberToFormalExpression(amount);

        Map<String, Object> summary = new HashMap<>();
        summary.put(SummaryFields.ACCOUNT_ACTIVITY, activityType.getValue());
        summary.put(SummaryFields.FULL_NAME, account.getCustomer().getFullName());
        summary.put(SummaryFields.NATIONAL_IDENTITY, account.getCustomer().getNationalId());
        summary.put(SummaryFields.ACCOUNT_IDENTITY, account.getId());
        summary.put(SummaryFields.AMOUNT, requestedAmountInSummary + " " + account.getCurrency());
        summary.put(SummaryFields.TRANSACTION_FEE, transactionFee);
        summary.put(SummaryFields.TIME,  LocalDateTime.now().toString());

        AccountActivity accountActivity = createAccountActivity(activityType, amount, summary, accounts, null);
        createAccountActivityForCharge(transactionFee, summary, accounts);

        cashFlowCalendarService.createCashFlow(account.getCustomer().getCashFlowCalendar(), accountActivity);
    }

    public void transferMoneyBetweenAccounts(MoneyTransferRequest request, Double amount, Account senderAccount, Account receiverAccount, Account chargedAccount) {
        AccountActivityType activityType = AccountActivityType.MONEY_TRANSFER;
        double transactionFee = getTransactionFee(activityType, List.of(senderAccount, receiverAccount));

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
        String requestedAmountInSummary = FormatterUtil.convertNumberToFormalExpression(amount);

        Map<String, Object> summary = new HashMap<>();
        summary.put(Entity.ACCOUNT_ACTIVITY.getValue(), activityType.getValue());
        summary.put(SummaryFields.FULL_NAME, senderAccount.getCustomer().getFullName());
        summary.put(SummaryFields.NATIONAL_IDENTITY,  senderAccount.getCustomer().getNationalId());
        summary.put("Sender " + SummaryFields.ACCOUNT_IDENTITY,  senderAccount.getId());
        summary.put("Receiver " + SummaryFields.ACCOUNT_IDENTITY,  receiverAccount.getId());
        summary.put(SummaryFields.AMOUNT,  requestedAmountInSummary + " " + senderAccount.getCurrency());
        summary.put(SummaryFields.TRANSACTION_FEE, transactionFee + " " + Currency.getChargeCurrency());
        summary.put(SummaryFields.PAYMENT_TYPE,  request.paymentType());
        summary.put(SummaryFields.TIME,  LocalDateTime.now().toString());

        AccountActivity accountActivity = createAccountActivity(activityType, request.amount(), summary, accounts, request.explanation());
        createAccountActivityForCharge(transactionFee, summary, accounts);

        if (!senderAccount.getCustomer().getNationalId().equals(receiverAccount.getCustomer().getNationalId())) {
            cashFlowCalendarService.createCashFlow(senderAccount.getCustomer().getCashFlowCalendar(), accountActivity);
            cashFlowCalendarService.createCashFlow(receiverAccount.getCustomer().getCashFlowCalendar(), accountActivity);
        }
    }

    public void exchangeMoneyBetweenAccounts(MoneyExchangeRequest request, Account sellerAccount, Account buyerAccount, Account chargedAccount) {
        AccountActivityType activityType = AccountActivityType.MONEY_EXCHANGE;
        Double rate = exchangeService.getBankExchangeRate(sellerAccount.getCurrency(), buyerAccount.getCurrency());
        Double spentAmount = request.amount();
        Double earnedAmount = exchangeService.convertMoneyBetweenCurrencies(sellerAccount.getCurrency(), buyerAccount.getCurrency(), spentAmount);
        Double transactionFee = getTransactionFee(activityType, List.of(sellerAccount, buyerAccount));

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
        summary.put(SummaryFields.NATIONAL_IDENTITY,  sellerAccount.getCustomer().getNationalId());
        summary.put("Seller " + SummaryFields.ACCOUNT_IDENTITY,  sellerAccount.getId());
        summary.put("Buyer " + SummaryFields.ACCOUNT_IDENTITY,  buyerAccount.getId());
        summary.put("Spent " + SummaryFields.AMOUNT,  spentAmountInSummary + " " + sellerAccount.getCurrency());
        summary.put("Earned " + SummaryFields.AMOUNT,  earnedAmountInSummary + " " + buyerAccount.getCurrency());
        summary.put(SummaryFields.RATE,  rate);
        summary.put(SummaryFields.TRANSACTION_FEE, transactionFee + " " + Currency.getChargeCurrency());
        summary.put(SummaryFields.TIME,  LocalDateTime.now().toString());

        createAccountActivity(activityType, earnedAmount, summary, accounts, null);
        createAccountActivityForCharge(transactionFee, summary, accounts);
    }

    public double getTransactionFee(AccountActivityType activityType, List<Account> accounts) {
        return switch (activityType) {
            case AccountActivityType.MONEY_TRANSFER -> {
                Account senderAccount = accounts.getFirst();
                Account receiverAccount = accounts.getLast();
                yield senderAccount.getCustomer()
                        .getNationalId()
                        .equals(receiverAccount.getCustomer().getNationalId()) ? 0
                        : chargeService.getAmountByActivityType(activityType);
            }
            case AccountActivityType.MONEY_DEPOSIT, AccountActivityType.WITHDRAWAL -> 0;
            case AccountActivityType.MONEY_EXCHANGE, AccountActivityType.FEE -> chargeService.getAmountByActivityType(activityType);
            default -> throw new ResourceConflictException(ResponseMessage.IMPROPER_ACCOUNT_ACTIVITY);
        };
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
}
