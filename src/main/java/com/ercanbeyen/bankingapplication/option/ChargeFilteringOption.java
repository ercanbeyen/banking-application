package com.ercanbeyen.bankingapplication.option;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public non-sealed class ChargeFilteringOption extends BaseFilteringOption {
    private Double amount;
}
