package com.ercanbeyen.bankingapplication.constant.enums;

public enum Currency {
    TL,
    USD,
    EUR,
    GBP,
    ALT;

    public static Currency getChargeCurrency() {
        return TL;
    }
}
