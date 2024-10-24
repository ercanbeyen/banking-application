package com.ercanbeyen.bankingapplication.option;

import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public non-sealed class DailyActivityLimitFilteringOptions extends BaseFilteringOptions {
    private AccountActivityType activityType;
    private Double amount;
}
