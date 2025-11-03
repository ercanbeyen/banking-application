package com.ercanbeyen.bankingapplication.dto.request;

import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import com.ercanbeyen.bankingapplication.entity.Account;

import java.util.Map;

public record AccountActivityRequest(
        AccountActivityType activityType,
        Account senderAccount,
        Account recipientAccount,
        Double amount,
        Map<String, Object> summary,
        String explanation) {

}
