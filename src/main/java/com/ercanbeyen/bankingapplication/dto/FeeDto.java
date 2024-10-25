package com.ercanbeyen.bankingapplication.dto;

import com.ercanbeyen.bankingapplication.constant.enums.Currency;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public non-sealed class FeeDto extends BaseDto {
    private Currency currency;
    private Double minimumAmount;
    private Double maximumAmount;
    private Double interestRatio;
    private Integer depositPeriod;
}
