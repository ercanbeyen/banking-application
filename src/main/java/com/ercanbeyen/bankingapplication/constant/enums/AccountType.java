package com.ercanbeyen.bankingapplication.constant.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AccountType {
    CURRENT("Current"),
    DEPOSIT("Deposit");

    private final String value;
}
