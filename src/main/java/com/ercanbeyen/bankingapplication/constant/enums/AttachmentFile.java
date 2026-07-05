package com.ercanbeyen.bankingapplication.constant.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AttachmentFile {
    ACCOUNT_STATEMENT("Account Statement"),
    FINANCIAL_STATUS_REPORT("Financial Status Report"),
    RECEIPT("Receipt");

    private final String value;
}
