package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import com.ercanbeyen.bankingapplication.constant.enums.BalanceActivity;
import com.ercanbeyen.bankingapplication.constant.enums.Currency;
import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.LogMessages;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessages;
import com.ercanbeyen.bankingapplication.constant.query.SummaryFields;
import com.ercanbeyen.bankingapplication.dto.request.AccountActivityRequest;
import com.ercanbeyen.bankingapplication.dto.request.ExchangeRequest;
import com.ercanbeyen.bankingapplication.dto.request.MoneyTransferRequest;
import com.ercanbeyen.bankingapplication.entity.Account;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import com.ercanbeyen.bankingapplication.repository.AccountRepository;
import com.ercanbeyen.bankingapplication.service.AccountActivityService;
import com.ercanbeyen.bankingapplication.util.FormatterUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javatuples.Pair;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TransactionService {
    private final AccountRepository accountRepository;
    private final AccountActivityService accountActivityService;
    private final ExchangeService exchangeService;
    private final ChargeService chargeService;

    public void updateBalanceOfSingleAccount(AccountActivityType activityType, Double amount, Account account) {
        Pair<BalanceActivity, Account[]> activityParameters = constructActivityParameters(activityType, account);

        int numberOfUpdatedEntities = accountRepository.updateBalanceById(account.getId(), activityParameters.getValue0().name(), amount);
        log.info(LogMessages.NUMBER_OF_UPDATED_ENTITIES, numberOfUpdatedEntities);

        Double transactionFee = chargeService.getAmountByActivityType(activityType);
        String requestedAmountInSummary = FormatterUtil.convertNumberToFormalExpression(amount);

        Map<String, Object> summary = new HashMap<>();
        summary.put(SummaryFields.ACCOUNT_ACTIVITY, activityType.getValue());
        summary.put(SummaryFields.FULL_NAME, account.getCustomer().getFullName());
        summary.put(SummaryFields.NATIONAL_IDENTITY, account.getCustomer().getNationalId());
        summary.put(SummaryFields.ACCOUNT_IDENTITY, account.getId());
        summary.put(SummaryFields.AMOUNT, requestedAmountInSummary + " " + account.getCurrency());
        summary.put(SummaryFields.TRANSACTION_FEE, transactionFee);
        summary.put(SummaryFields.TIME,  LocalDateTime.now().toString());

        createAccountActivity(activityType, amount, summary, activityParameters.getValue1(), null);
        createAccountActivityForCharge(transactionFee, summary, activityParameters.getValue1());
    }

    public void transferMoneyBetweenAccounts(MoneyTransferRequest request, Double amount, Account senderAccount, Account receiverAccount, Account chargedAccount) {
        AccountActivityType activityType = AccountActivityType.MONEY_TRANSFER;
        double transactionFee = chargeService.getAmountByActivityType(activityType);
        int numberOfUpdatedEntities;

        if (Optional.ofNullable(chargedAccount).isEmpty()) {
            numberOfUpdatedEntities = accountRepository.updateBalanceById(senderAccount.getId(), BalanceActivity.DECREASE.name(), amount + transactionFee);
            log.info(LogMessages.NUMBER_OF_UPDATED_ENTITIES, numberOfUpdatedEntities);
        } else {
            numberOfUpdatedEntities = accountRepository.updateBalanceById(senderAccount.getId(), BalanceActivity.DECREASE.name(), amount);
            log.info(LogMessages.NUMBER_OF_UPDATED_ENTITIES, numberOfUpdatedEntities);
            numberOfUpdatedEntities = accountRepository.updateBalanceById(chargedAccount.getId(), BalanceActivity.DECREASE.name(), transactionFee);
            log.info(LogMessages.NUMBER_OF_UPDATED_ENTITIES, numberOfUpdatedEntities);
        }

        numberOfUpdatedEntities = accountRepository.updateBalanceById(receiverAccount.getId(), BalanceActivity.INCREASE.name(), amount);
        log.info(LogMessages.NUMBER_OF_UPDATED_ENTITIES, numberOfUpdatedEntities);

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

        createAccountActivity(activityType, request.amount(), summary, accounts, request.explanation());
        createAccountActivityForCharge(transactionFee, summary, accounts);
    }

    public void exchangeMoneyBetweenAccounts(ExchangeRequest request, Account sellerAccount, Account buyerAccount, Account chargedAccount) {
        AccountActivityType activityType = AccountActivityType.MONEY_EXCHANGE;
        Double rate = exchangeService.getBankExchangeRate(sellerAccount.getCurrency(), buyerAccount.getCurrency());
        Double spentAmount = request.amount();
        Double earnedAmount = exchangeService.exchangeMoneyBetweenAccounts(sellerAccount, buyerAccount, spentAmount);
        Double transactionFee = chargeService.getAmountByActivityType(activityType);

        int numberOfUpdatedEntities;
        int chargedAccountId;

        if (Optional.ofNullable(chargedAccount).isEmpty()) {
            chargedAccountId = sellerAccount.getCurrency() == Currency.getChargeCurrency()
                    ? sellerAccount.getId()
                    : buyerAccount.getId();
        } else {
            chargedAccountId = chargedAccount.getId();
        }

        numberOfUpdatedEntities = accountRepository.updateBalanceById(sellerAccount.getId(), BalanceActivity.DECREASE.name(), spentAmount);
        log.info(LogMessages.NUMBER_OF_UPDATED_ENTITIES, numberOfUpdatedEntities);

        numberOfUpdatedEntities = accountRepository.updateBalanceById(chargedAccountId, BalanceActivity.DECREASE.name(), transactionFee);
        log.info(LogMessages.NUMBER_OF_UPDATED_ENTITIES, numberOfUpdatedEntities);

        numberOfUpdatedEntities = accountRepository.updateBalanceById(request.buyerAccountId(), BalanceActivity.INCREASE.name(), earnedAmount);
        log.info(LogMessages.NUMBER_OF_UPDATED_ENTITIES, numberOfUpdatedEntities);

        String spentAmountInSummary = FormatterUtil.convertNumberToFormalExpression(spentAmount);
        log.info(LogMessages.PROCESSED_AMOUNT, spentAmountInSummary, "Spent");

        String earnedAmountInSummary = FormatterUtil.convertNumberToFormalExpression(earnedAmount);
        log.info(LogMessages.PROCESSED_AMOUNT, earnedAmountInSummary, "Earn");

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

    private void createAccountActivityForCharge(Double transactionFee, Map<String, Object> summary, Account[] accounts) {
        if (transactionFee == 0) {
            log.warn("There is no transaction fee");
            return;
        }

        createAccountActivity(AccountActivityType.CHARGE, transactionFee, summary, accounts, null);
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
