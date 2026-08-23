package com.ercanbeyen.bankingapplication.service;

import com.ercanbeyen.bankingapplication.constant.enums.*;
import com.ercanbeyen.bankingapplication.dto.AccountDto;
import com.ercanbeyen.bankingapplication.dto.request.AccountActivityFilteringRequest;
import com.ercanbeyen.bankingapplication.dto.request.MoneyExchangeRequest;
import com.ercanbeyen.bankingapplication.dto.request.MoneyTransferRequest;
import com.ercanbeyen.bankingapplication.dto.response.AccountActivityPreview;
import com.ercanbeyen.bankingapplication.dto.response.CustomerStatisticsResponse;
import com.ercanbeyen.bankingapplication.entity.Account;
import com.ercanbeyen.bankingapplication.dto.option.AccountFilteringOption;

import java.util.List;

public interface AccountService extends BaseService<AccountDto, AccountFilteringOption> {
    void depositMoney(Integer id, Double amount, Channel channel);
    void withdrawMoney(Integer id, Double amount, Channel channel);
    String payInterestIncome(Integer id);
    void transferMoney(MoneyTransferRequest request, Channel channel);
    void exchangeMoney(MoneyExchangeRequest request, Channel channel);
    String updateBlockStatus(Integer id, boolean status);
    void closeAccount(Integer id);
    Integer getTotalActiveAccounts(AccountType type, Currency currency, String city);
    List<CustomerStatisticsResponse> getCustomersHaveMaximumBalance(AccountType type, Currency currency);
    List<AccountActivityPreview> getAccountActivityPreviews(Integer id, AccountActivityFilteringRequest request);
    Account getDeducteeAccount(AccountActivityType accountActivityType, Integer extraDeducteeAccountId, List<Account> relatedAccounts);
    Account findDeducteeAccountById(Integer id);
    Account findActiveAccountById(Integer id);
    Account findById(Integer id);
    void checkAccountsBeforeMoneyTransfer(Account senderAccount, Account recipientAccount);
}
