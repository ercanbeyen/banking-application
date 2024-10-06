package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import com.ercanbeyen.bankingapplication.constant.enums.BalanceActivity;
import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.LogMessages;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessages;
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


        summary.put(Entity.ACCOUNT_ACTIVITY.getValue(), activityType.getValue());
        summary.put("Customer national identity", account.getCustomer().getNationalId());
        summary.put("Account identity", account.getId());
        summary.put("Amount", requestedAmountInSummary + account.getCurrency());
        summary.put("Time",  LocalDateTime.now().toString());

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
        summary.put("Customer national identity",  senderAccount.getCustomer().getNationalId());
        summary.put("Sender account identity",  senderAccount.getId());
        summary.put("Receiver account identity",  receiverAccount.getId());
        summary.put("Amount",  requestedAmountInSummary + senderAccount.getCurrency());
        summary.put("Payment type",  request.paymentType());
        summary.put("Time",  LocalDateTime.now().toString());

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
        summary.put("Customer national identity",  sellerAccount.getCustomer().getNationalId());
        summary.put("Seller account identity",  sellerAccount.getId());
        summary.put("Buyer account identity",  buyerAccount.getId());
        summary.put("Spent amount",  spentAmountInSummary + sellerAccount.getCurrency());
        summary.put("Earned amount",  earnedAmountInSummary + buyerAccount.getCurrency());
        summary.put("Time",  LocalDateTime.now());

        createAccountActivity(activityType, earnedAmount, summary, accounts, null);
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
