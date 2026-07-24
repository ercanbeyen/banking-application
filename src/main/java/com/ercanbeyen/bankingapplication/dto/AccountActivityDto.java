package com.ercanbeyen.bankingapplication.dto;

import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import com.ercanbeyen.bankingapplication.constant.enums.Channel;

import java.time.LocalDateTime;
import java.util.Map;

public record AccountActivityDto(
        String id,
        AccountActivityType type,
        Integer senderAccountId,
        Integer recipientAccountId,
        Double amount,
        Map<String, Object> summary,
        String explanation,
        Channel channel,
        LocalDateTime createdAt) {

}
