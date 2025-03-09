package com.ercanbeyen.bankingapplication.dto;

import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.time.LocalDateTime;

public record DailyActivityLimitDto(
        String id,
        @NotNull(message = "Activity type should not be null")
        AccountActivityType activityType,
        @NotNull(message = "Amount should not be null")
        @Min(value = 0, message = "Amount should be at least {value}")
        Double amount,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime modifiedAt) implements Serializable {

}
