package com.ercanbeyen.bankingapplication.option;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public non-sealed class ChargeFilteringOptions extends BaseFilteringOptions {
    private Double amount;
}
