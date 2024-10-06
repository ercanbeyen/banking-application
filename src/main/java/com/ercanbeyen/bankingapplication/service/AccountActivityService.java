package com.ercanbeyen.bankingapplication.service;

import com.ercanbeyen.bankingapplication.constant.enums.Currency;
import com.ercanbeyen.bankingapplication.dto.AccountActivityDto;
import com.ercanbeyen.bankingapplication.dto.request.AccountActivityRequest;
import com.ercanbeyen.bankingapplication.view.entity.AccountActivityView;
import com.ercanbeyen.bankingapplication.option.AccountActivityFilteringOptions;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Set;

public interface AccountActivityService {
    List<AccountActivityDto> getAccountActivities(AccountActivityFilteringOptions options);
    Set<AccountActivityDto> getAccountActivitiesOfParticularAccounts(AccountActivityFilteringOptions options, Currency currency);
    AccountActivityDto getAccountActivity(String id);
    void createAccountActivity(AccountActivityRequest request);
    List<AccountActivityView> getAccountActivityViews(Integer senderAccountId, Integer receiverAccountId);
    ByteArrayOutputStream generateReceiptPdfStream(String id);
}
