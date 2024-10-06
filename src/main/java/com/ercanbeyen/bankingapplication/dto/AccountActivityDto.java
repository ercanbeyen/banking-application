package com.ercanbeyen.bankingapplication.dto;

import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;

import java.time.LocalDateTime;
import java.util.Map;

public record AccountActivityDto(
        String id,
        AccountActivityType type,
        Integer senderAccountId,
        Integer receiverAccountId,
        Double amount,
        LocalDateTime createdAt,
        Map<String, Object> summary,
        String explanation) {

}
