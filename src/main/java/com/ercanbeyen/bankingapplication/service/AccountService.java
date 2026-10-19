package com.ercanbeyen.bankingapplication.service;

import com.ercanbeyen.bankingapplication.constant.enums.*;
import com.ercanbeyen.bankingapplication.dto.AccountDto;
import com.ercanbeyen.bankingapplication.dto.BranchOrderInformation;
import com.ercanbeyen.bankingapplication.dto.ChannelInformation;
import com.ercanbeyen.bankingapplication.dto.request.AccountActivityFilteringRequest;
import com.ercanbeyen.bankingapplication.dto.request.MoneyExchangeRequest;
import com.ercanbeyen.bankingapplication.dto.request.MoneyTransferRequest;
import com.ercanbeyen.bankingapplication.dto.response.AccountActivityPreview;
import com.ercanbeyen.bankingapplication.dto.response.CustomerStatisticsResponse;
import com.ercanbeyen.bankingapplication.entity.Account;
import com.ercanbeyen.bankingapplication.dto.option.AccountFilteringOption;

import java.util.List;

public interface AccountService extends BaseService<AccountDto, AccountFilteringOption> {
    void depositMoney(Integer id, ActivityType activityType, Double amount, ChannelInformation channelInformation, BranchOrderInformation branchOrderInformation);
    void withdrawMoney(Integer id, ActivityType activityType, Double amount, ChannelInformation channelInformation, BranchOrderInformation branchOrderInformation);
    String payInterestIncome(Integer id);
    void transferMoney(MoneyTransferRequest request, ChannelInformation channelInformation, BranchOrderInformation branchOrderInformation);
    void exchangeMoney(MoneyExchangeRequest request, ChannelInformation channelInformation, BranchOrderInformation branchOrderInformation);
    void updateBlockStatus(Integer id, boolean status);
    void closeAccount(Integer id);
    Integer getTotalActiveAccounts(AccountType type, Currency currency, String city);
    List<CustomerStatisticsResponse> getCustomersHaveMaximumBalance(AccountType type, Currency currency);
    List<AccountActivityPreview> getAccountActivityPreviews(Integer id, AccountActivityFilteringRequest request);
    Account getDeducteeAccount(ActivityType activityType, Integer extraDeducteeAccountId, List<Account> accounts);
    Account findActiveAccountById(Integer id);
    Account findById(Integer id);
    void checkAccountsBeforeMoneyTransfer(Account senderAccount, Account recipientAccount);
}
