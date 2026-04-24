package com.ercanbeyen.bankingapplication.dto.option;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Data
public non-sealed class CustomerFilteringOption extends BaseFilteringOption {
    LocalDate birthDate;
}
