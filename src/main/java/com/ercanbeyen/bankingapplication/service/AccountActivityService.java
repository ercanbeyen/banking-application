package com.ercanbeyen.bankingapplication.service;

import com.ercanbeyen.bankingapplication.constant.enums.Currency;
import com.ercanbeyen.bankingapplication.dto.AccountActivityDto;
import com.ercanbeyen.bankingapplication.dto.request.AccountActivityRequest;
import com.ercanbeyen.bankingapplication.entity.AccountActivity;
import com.ercanbeyen.bankingapplication.view.entity.AccountActivityView;
import com.ercanbeyen.bankingapplication.option.AccountActivityFilteringOption;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Set;

public interface AccountActivityService {
    List<AccountActivityDto> getAccountActivities(AccountActivityFilteringOption option);
    Set<AccountActivityDto> getAccountActivitiesOfParticularAccounts(AccountActivityFilteringOption option, Currency currency);
    AccountActivityDto getAccountActivity(String id);
    AccountActivity createAccountActivity(AccountActivityRequest request);
    List<AccountActivityView> getAccountActivityViews(Integer senderAccountId, Integer receiverAccountId);
    ByteArrayOutputStream createReceiptStream(String id);
    boolean existsByIdAndCustomerNationalId(String id, String customerNationalId);
}
