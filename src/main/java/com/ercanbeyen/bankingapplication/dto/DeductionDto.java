package com.ercanbeyen.bankingapplication.dto;


import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.time.Instant;

public record DeductionDto(
        String id,
        @NotNull(message = "Activity type should not be null")
        AccountActivityType activityType,
        @NotNull(message = "Minimum amount should not be null")
        @Min(value = 0, message = "Amount should be at least {value}")
        Double amount,
        @JsonFormat(
                shape = JsonFormat.Shape.STRING,
                pattern = "yyyy-MM-dd HH:mm:ss",
                timezone = "UTC"
        )
        Instant modifiedAt) implements Serializable {

}
