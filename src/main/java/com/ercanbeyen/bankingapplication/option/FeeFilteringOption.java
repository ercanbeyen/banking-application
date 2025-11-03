package com.ercanbeyen.bankingapplication.option;

import com.ercanbeyen.bankingapplication.constant.enums.Currency;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public non-sealed class FeeFilteringOption extends BaseFilteringOption {
    private Currency currency;
    private Integer depositPeriod;
}
