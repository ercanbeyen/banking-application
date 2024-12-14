package com.ercanbeyen.bankingapplication.option;

import com.ercanbeyen.bankingapplication.constant.enums.Currency;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public non-sealed class FeeFilteringOption extends BaseFilteringOption {
    private Currency currency;
    private Integer depositPeriod;
}
