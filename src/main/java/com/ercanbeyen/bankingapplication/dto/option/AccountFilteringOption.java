package com.ercanbeyen.bankingapplication.dto.option;

import com.ercanbeyen.bankingapplication.constant.enums.AccountType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Data
public non-sealed class AccountFilteringOption extends BaseFilteringOption {
    private AccountType type;
    private LocalDate depositMaturityDate;
    private Boolean isBlocked;
    private Boolean isClosed;
}
