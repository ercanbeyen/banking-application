package com.ercanbeyen.bankingapplication.dto;

import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public non-sealed class DailyActivityLimitDto extends BaseDto {
    @NotNull(message = "Activity type should not be null")
    private AccountActivityType activityType;
    @NotNull(message = "Amount should not be null")
    @Min(value = 0, message = "Amount should be at least {value}")
    private Double amount;
}
