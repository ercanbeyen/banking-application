package com.ercanbeyen.bankingapplication.constant.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AgreementSubject {
    CUSTOMER("Customer"),
    DEPOSIT_ACCOUNT("Deposit Account");

    private final String value;
}
