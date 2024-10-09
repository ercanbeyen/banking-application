package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import com.ercanbeyen.bankingapplication.constant.enums.BalanceActivity;
import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.LogMessages;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessages;
import com.ercanbeyen.bankingapplication.constant.query.SummaryFields;
import com.ercanbeyen.bankingapplication.dto.request.AccountActivityRequest;
import com.ercanbeyen.bankingapplication.dto.request.ExchangeRequest;
import com.ercanbeyen.bankingapplication.dto.request.TransferRequest;
import com.ercanbeyen.bankingapplication.entity.Account;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import com.ercanbeyen.bankingapplication.repository.AccountRepository;
import com.ercanbeyen.bankingapplication.service.AccountActivityService;
import com.ercanbeyen.bankingapplication.util.NumberFormatterUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javatuples.Pair;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TransactionService {
    private final AccountRepository accountRepository;
    private final AccountActivityService accountActivityService;
    private final ExchangeService exchangeService;

    public void updateBalanceOfSingleAccount(AccountActivityType activityType, Double amount, Account account) {
        Pair<BalanceActivity, Account[]> activityParameters = constructActivityParameters(activityType, account);

        int numberOfUpdatedEntities = accountRepository.updateBalanceById(account.getId(), activityParameters.getValue0().name(), amount);
        log.info(LogMessages.NUMBER_OF_UPDATED_ENTITIES, numberOfUpdatedEntities);

        String requestedAmountInSummary = NumberFormatterUtil.convertNumberToFormalExpression(amount);

        Map<String, Object> summary = new HashMap<>();
        summary.put(SummaryFields.ACCOUNT_ACTIVITY, activityType.getValue());
        summary.put(SummaryFields.FULL_NAME, account.getCustomer().getFullName());
        summary.put(SummaryFields.NATIONAL_IDENTITY, account.getCustomer().getNationalId());
        summary.put(SummaryFields.ACCOUNT_IDENTITY, account.getId());
        summary.put(SummaryFields.AMOUNT, requestedAmountInSummary + " " + account.getCurrency());
        summary.put(SummaryFields.TIME,  LocalDateTime.now().toString());

        createAccountActivity(activityType, amount, summary, activityParameters.getValue1(), null);
    }

    public void transferMoneyBetweenAccounts(TransferRequest request, Integer senderAccountId, Double amount, Integer receiverAccountId, Account senderAccount, Account receiverAccount) {
        int numberOfUpdatedEntities = accountRepository.updateBalanceById(senderAccountId, BalanceActivity.DECREASE.name(), amount);
        log.info(LogMessages.NUMBER_OF_UPDATED_ENTITIES, numberOfUpdatedEntities);

        numberOfUpdatedEntities = accountRepository.updateBalanceById(receiverAccountId, BalanceActivity.INCREASE.name(), amount);
        log.info(LogMessages.NUMBER_OF_UPDATED_ENTITIES, numberOfUpdatedEntities);

        Account[] accounts = {senderAccount, receiverAccount};
        String requestedAmountInSummary = NumberFormatterUtil.convertNumberToFormalExpression(amount);

        AccountActivityType activityType = AccountActivityType.MONEY_TRANSFER;

        Map<String, Object> summary = new HashMap<>();
        summary.put(Entity.ACCOUNT_ACTIVITY.getValue(), activityType.getValue());
        summary.put(SummaryFields.FULL_NAME, senderAccount.getCustomer().getFullName());
        summary.put(SummaryFields.NATIONAL_IDENTITY,  senderAccount.getCustomer().getNationalId());
        summary.put("Sender " + SummaryFields.ACCOUNT_IDENTITY,  senderAccount.getId());
        summary.put("Receiver " + SummaryFields.ACCOUNT_IDENTITY,  receiverAccount.getId());
        summary.put(SummaryFields.AMOUNT,  requestedAmountInSummary + " " + senderAccount.getCurrency());
        summary.put(SummaryFields.PAYMENT_TYPE,  request.paymentType());
        summary.put(SummaryFields.TIME,  LocalDateTime.now().toString());

        createAccountActivity(activityType, request.amount(), summary, accounts, request.explanation());
    }

    public void exchangeMoneyBetweenAccounts(ExchangeRequest request, Account sellerAccount, Account buyerAccount) {
        Double spentAmount = request.amount();
        Double earnedAmount = exchangeService.exchangeMoneyBetweenAccounts(sellerAccount, buyerAccount, spentAmount);

        int numberOfUpdatedEntities = accountRepository.updateBalanceById(request.sellerId(), BalanceActivity.DECREASE.name(), spentAmount);
        log.info(LogMessages.NUMBER_OF_UPDATED_ENTITIES, numberOfUpdatedEntities);

        numberOfUpdatedEntities = accountRepository.updateBalanceById(request.buyerId(), BalanceActivity.INCREASE.name(), earnedAmount);
        log.info(LogMessages.NUMBER_OF_UPDATED_ENTITIES, numberOfUpdatedEntities);

        String spentAmountInSummary = NumberFormatterUtil.convertNumberToFormalExpression(spentAmount);
        log.info(LogMessages.PROCESSED_AMOUNT, spentAmountInSummary, "Spent");

        String earnedAmountInSummary = NumberFormatterUtil.convertNumberToFormalExpression(earnedAmount);
        log.info(LogMessages.PROCESSED_AMOUNT, earnedAmountInSummary, "Earn");

        AccountActivityType activityType = AccountActivityType.MONEY_EXCHANGE;

        Account[] accounts = {sellerAccount, buyerAccount};

        Map<String, Object> summary = new HashMap<>();
        summary.put(Entity.ACCOUNT_ACTIVITY.getValue(), activityType.getValue());
        summary.put(SummaryFields.FULL_NAME, sellerAccount.getCustomer().getFullName());
        summary.put(SummaryFields.NATIONAL_IDENTITY,  sellerAccount.getCustomer().getNationalId());
        summary.put("Seller " + SummaryFields.ACCOUNT_IDENTITY,  sellerAccount.getId());
        summary.put("Buyer " + SummaryFields.ACCOUNT_IDENTITY,  buyerAccount.getId());
        summary.put("Spent " + SummaryFields.AMOUNT,  spentAmountInSummary + " " + sellerAccount.getCurrency());
        summary.put("Earned " + SummaryFields.AMOUNT,  earnedAmountInSummary + " " + buyerAccount.getCurrency());
        summary.put(SummaryFields.TIME,  LocalDateTime.now());

        createAccountActivity(activityType, earnedAmount, summary, accounts, null);
    }

    public void createAccountActivityForAccountOpeningAndClosing(Account account, AccountActivityType activityType) {
        Map<String, Object> summary = new HashMap<>();
        summary.put(SummaryFields.ACCOUNT_ACTIVITY, activityType.getValue());
        summary.put(SummaryFields.FULL_NAME, account.getCustomer().getFullName());
        summary.put(SummaryFields.NATIONAL_IDENTITY, account.getCustomer().getNationalId());
        summary.put(SummaryFields.ACCOUNT_TYPE, account.getCurrency() + " " + account.getType());
        summary.put(SummaryFields.BRANCH, account.getBranch().getName());
        summary.put(SummaryFields.TIME, LocalDateTime.now().toString());

        AccountActivityRequest request = new AccountActivityRequest(
                activityType,
                null,
                null,
                0D,
                summary,
                null
        );

        accountActivityService.createAccountActivity(request);
    }

    private void createAccountActivity(AccountActivityType activityType, Double amount, Map<String, Object> summary, Account[] accounts, String explanation) {
        AccountActivityRequest accountActivityRequest = new AccountActivityRequest(activityType, accounts[0], accounts[1], amount, summary, explanation);
        accountActivityService.createAccountActivity(accountActivityRequest);
    }

    /***
     *
     * @param activityType is for determining account activity type
     * @param account represents the responsible account
     * @return balance activity and position of the account (is account sender or receiver?)
     */
    private static Pair<BalanceActivity, Account[]> constructActivityParameters(AccountActivityType activityType, Account account) {
        Account[] accounts = new Account[2]; // first account is sender, second account is receiver
        BalanceActivity balanceActivity;

        switch (activityType) {
            case AccountActivityType.MONEY_DEPOSIT, AccountActivityType.FEE -> {
                accounts[1] = account; // receiver
                balanceActivity = BalanceActivity.INCREASE;
            }
            case AccountActivityType.WITHDRAWAL, AccountActivityType.CHARGE -> {
                accounts[0] = account; // sender
                balanceActivity = BalanceActivity.DECREASE;
            }
            default -> throw new ResourceConflictException(ResponseMessages.IMPROPER_ACCOUNT_ACTIVITY);
        }

        return new Pair<>(balanceActivity, accounts);
    }
}
