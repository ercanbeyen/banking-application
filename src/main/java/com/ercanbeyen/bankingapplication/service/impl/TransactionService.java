package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import com.ercanbeyen.bankingapplication.constant.enums.BalanceActivity;
import com.ercanbeyen.bankingapplication.constant.enums.Currency;
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

        String summaryTemplate = """
                Account Activity: %s
                Customer National Id: %s
                Account Id: %s
                Amount: %s %s
                Time: %s
                """;

        String summary = String.format(
                summaryTemplate,
                activityType.getValue(),
                account.getCustomer().getNationalId(),
                account.getId(),
                requestedAmountInSummary,
                account.getCurrency(),
                LocalDateTime.now()
        );

        createAccountActivity(activityType, amount, summary, activityParameters.getValue1(), null);
    }

    public void transferMoneyBetweenAccounts(TransferRequest request, Integer senderAccountId, Double amount, Integer receiverAccountId, Account senderAccount, Account receiverAccount) {
        int numberOfUpdatedEntities = accountRepository.updateBalanceById(senderAccountId, BalanceActivity.DECREASE.name(), amount);
        log.info(LogMessages.NUMBER_OF_UPDATED_ENTITIES, numberOfUpdatedEntities);

        numberOfUpdatedEntities = accountRepository.updateBalanceById(receiverAccountId, BalanceActivity.INCREASE.name(), amount);
        log.info(LogMessages.NUMBER_OF_UPDATED_ENTITIES, numberOfUpdatedEntities);

        Account[] accounts = {senderAccount, receiverAccount};
        String requestedAmountInSummary = NumberFormatterUtil.convertNumberToFormalExpression(amount);

        String summaryTemplate = """
                Account Activity: %s
                Customer National Id: %s
                From Account Id: %s
                To Account Id: %s
                Amount: %s %s
                Payment Type: %s
                Time: %s
                """;

        AccountActivityType activityType = AccountActivityType.MONEY_TRANSFER;

        String summary = String.format(
                summaryTemplate,
                activityType.getValue(),
                senderAccount.getCustomer().getNationalId(),
                senderAccount.getId(),
                receiverAccount.getId(),
                requestedAmountInSummary,
                receiverAccount.getCurrency(),
                request.paymentType(),
                LocalDateTime.now()
        );

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

        String summaryTemplate = """
                Account Activity: %s
                From Account Id: %s
                To Account Id: %s
                Spent amount: %s %s
                Earned amount: %s %s
                Time: %s
                """;

        AccountActivityType activityType = AccountActivityType.MONEY_EXCHANGE;
        Currency sellerAccountCurrency = sellerAccount.getCurrency();
        Currency buyerAccountCurrency = buyerAccount.getCurrency();

        String summary = String.format(
                summaryTemplate,
                activityType.getValue(),
                sellerAccount.getId(),
                buyerAccount.getId(),
                spentAmountInSummary,
                sellerAccountCurrency,
                earnedAmountInSummary,
                buyerAccountCurrency,
                LocalDateTime.now()
        );

        Account[] accounts = {sellerAccount, buyerAccount};

        createAccountActivity(activityType, earnedAmount, summary, accounts, null);
    }

    private void createAccountActivity(AccountActivityType activityType, Double amount, String summary, Account[] accounts, String explanation) {
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
