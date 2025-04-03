package com.ercanbeyen.bankingapplication.service;

import com.ercanbeyen.bankingapplication.constant.enums.AccountType;
import com.ercanbeyen.bankingapplication.constant.enums.City;
import com.ercanbeyen.bankingapplication.constant.enums.Currency;
import com.ercanbeyen.bankingapplication.dto.AccountDto;
import com.ercanbeyen.bankingapplication.dto.request.MoneyExchangeRequest;
import com.ercanbeyen.bankingapplication.dto.request.MoneyTransferRequest;
import com.ercanbeyen.bankingapplication.dto.response.CustomerStatisticsResponse;
import com.ercanbeyen.bankingapplication.entity.Account;
import com.ercanbeyen.bankingapplication.option.AccountFilteringOption;

import java.util.List;

public interface AccountService extends BaseService<AccountDto, AccountFilteringOption> {
    String depositMoney(Integer id, Double amount);
    String withdrawMoney(Integer id, Double amount);
    String payInterest(Integer id);
    String transferMoney(MoneyTransferRequest request);
    String exchangeMoney(MoneyExchangeRequest request);
    String updateBlockStatus(Integer id, boolean status);
    String closeAccount(Integer id);
    String getTotalActiveAccounts(AccountType type, Currency currency, City city);
    List<CustomerStatisticsResponse> getCustomersHaveMaximumBalance(AccountType type, Currency currency);
    Account getChargedAccount(Integer extraChargedAccountId, List<Account> relatedAccounts);
    Account findChargedAccountById(Integer id);
    Account findActiveAccountById(Integer id);
    void checkAccountsBeforeMoneyTransfer(Account senderAccount, Account receiverAccount);
}
