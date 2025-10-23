package com.ercanbeyen.bankingapplication.option;

import com.ercanbeyen.bankingapplication.constant.enums.AccountType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public non-sealed class AccountFilteringOption extends BaseFilteringOption {
    private AccountType type;
    private Boolean isBlocked;
    private Boolean isClosed;
}
