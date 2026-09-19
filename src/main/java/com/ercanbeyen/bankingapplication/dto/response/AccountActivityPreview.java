package com.ercanbeyen.bankingapplication.dto.response;

import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import com.ercanbeyen.bankingapplication.constant.enums.BalanceActivity;
import com.ercanbeyen.bankingapplication.constant.enums.ChannelType;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record AccountActivityPreview(
        String accountActivityId,
        AccountActivityType accountActivityType,
        BalanceActivity balanceActivity,
        Double amount,
        ChannelType channelType,
        @JsonFormat(
                shape = JsonFormat.Shape.STRING,
                pattern = "yyyy-MM-dd HH:mm:ss"
        )
        LocalDateTime createdAt) {
}
