package com.ercanbeyen.bankingapplication.service;

import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import com.ercanbeyen.bankingapplication.constant.enums.ChannelType;
import com.ercanbeyen.bankingapplication.dto.request.MoneyExchangeRequest;
import com.ercanbeyen.bankingapplication.dto.request.MoneyTransferRequest;
import com.ercanbeyen.bankingapplication.entity.Account;

public interface TransactionService {
    void createAccountActivityForAccountStatusUpdate(Account account, AccountActivityType activityType);
    void applyAccountActivityForSingleAccount(AccountActivityType activityType, Double amount, Account account, String cashFlowExplanation, ChannelType channelType);
    void transferMoneyBetweenAccounts(MoneyTransferRequest request, Double amount, Account senderAccount, Account recipientAccount, Account deducteeAccount, ChannelType channelType);
    void exchangeMoneyBetweenAccounts(MoneyExchangeRequest request, Account sellerAccount, Account buyerAccount, Account deducteeAccount, ChannelType channelType);
    void updateDepositAccountFields(Account account, double balance, int depositMaturity);

}
