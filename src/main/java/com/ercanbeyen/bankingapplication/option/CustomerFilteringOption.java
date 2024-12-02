package com.ercanbeyen.bankingapplication.option;

import lombok.Data;

import java.time.LocalDate;

@Data
public non-sealed class CustomerFilteringOption extends BaseFilteringOption {
    LocalDate birthDate;
}
