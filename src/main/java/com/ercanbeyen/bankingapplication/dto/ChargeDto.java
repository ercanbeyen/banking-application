package com.ercanbeyen.bankingapplication.dto;


import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public non-sealed class ChargeDto extends BaseDto {
    private AccountActivityType activityType;
    private Double amount;
}
