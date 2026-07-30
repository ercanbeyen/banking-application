package com.ercanbeyen.bankingapplication.dto;

import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.time.Instant;

public record DailyAccountActivityLimitDto(
        String id,
        @NotNull(message = "Activity type should not be null")
        AccountActivityType activityType,
        @NotNull(message = "Upper limit should not be null")
        @Min(value = 0, message = "Upper limit should be at least {value}")
        Double lowerLimit,
        @NotNull(message = "Upper limit should not be null")
        @Min(value = 0, message = "Upper limit should be at least {value}")
        Double upperLimit,
        @JsonFormat(
                shape = JsonFormat.Shape.STRING,
                pattern = "yyyy-MM-dd HH:mm:ss",
                timezone = "UTC"
        )
        Instant modifiedAt) implements Serializable {

}
