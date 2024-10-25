package com.ercanbeyen.bankingapplication.option;

import com.ercanbeyen.bankingapplication.constant.enums.Currency;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public non-sealed class FeeFilteringOptions extends BaseFilteringOptions {
    private Currency currency;
    private Double minimumAmount;
    private Double maximumAmount;
}
