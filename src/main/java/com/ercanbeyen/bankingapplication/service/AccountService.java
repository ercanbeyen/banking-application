package com.ercanbeyen.bankingapplication.service;

import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import com.ercanbeyen.bankingapplication.constant.enums.AccountType;
import com.ercanbeyen.bankingapplication.constant.enums.City;
import com.ercanbeyen.bankingapplication.constant.enums.Currency;
import com.ercanbeyen.bankingapplication.dto.AccountActivityDto;
import com.ercanbeyen.bankingapplication.dto.AccountDto;
import com.ercanbeyen.bankingapplication.dto.request.AccountActivityFilteringRequest;
import com.ercanbeyen.bankingapplication.dto.request.MoneyExchangeRequest;
import com.ercanbeyen.bankingapplication.dto.request.MoneyTransferRequest;
import com.ercanbeyen.bankingapplication.dto.response.CustomerStatisticsResponse;
import com.ercanbeyen.bankingapplication.entity.Account;
import com.ercanbeyen.bankingapplication.dto.option.AccountFilteringOption;

import java.util.List;

public interface AccountService extends BaseService<AccountDto, AccountFilteringOption> {
    void depositMoney(Integer id, Double amount);
    void withdrawMoney(Integer id, Double amount);
    String payInterestIncome(Integer id);
    void transferMoney(MoneyTransferRequest request);
    void exchangeMoney(MoneyExchangeRequest request);
    String updateBlockStatus(Integer id, boolean status);
    void closeAccount(Integer id);
    Integer getTotalActiveAccounts(AccountType type, Currency currency, City city);
    List<CustomerStatisticsResponse> getCustomersHaveMaximumBalance(AccountType type, Currency currency);
    Account getDeducteeAccount(AccountActivityType accountActivityType, Integer extraDeducteeAccountId, List<Account> relatedAccounts);
    Account findDeducteeAccountById(Integer id);
    Account findActiveAccountById(Integer id);
    void checkAccountsBeforeMoneyTransfer(Account senderAccount, Account recipientAccount);
    List<AccountActivityDto> getAccountActivities(Integer id, AccountActivityFilteringRequest request);
}
