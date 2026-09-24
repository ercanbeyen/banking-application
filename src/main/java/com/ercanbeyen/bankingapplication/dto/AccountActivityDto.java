package com.ercanbeyen.bankingapplication.dto;

import com.ercanbeyen.bankingapplication.constant.enums.ActivityType;
import com.ercanbeyen.bankingapplication.constant.enums.ChannelType;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.Map;

public record AccountActivityDto(
        String id,
        ActivityType type,
        Integer senderAccountId,
        Integer recipientAccountId,
        Double amount,
        Map<String, Object> summary,
        String explanation,
        ChannelType channelType,
        @JsonFormat(
                shape = JsonFormat.Shape.STRING,
                pattern = "yyyy-MM-dd HH:mm:ss",
                timezone = "UTC"
        )
        Instant createdAt) {

}
