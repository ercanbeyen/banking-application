package com.ercanbeyen.bankingapplication.option;

import com.ercanbeyen.bankingapplication.constant.enums.Currency;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public final class ExchangeFilteringOption extends BaseFilteringOption {
    private Currency targetCurrency;
    private Currency sourceCurrency;
    private Double rate;
}
