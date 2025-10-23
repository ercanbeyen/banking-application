package com.ercanbeyen.bankingapplication.constant.enums;

public enum Currency {
    TRY,
    USD,
    EUR,
    GBP;

    public static Currency getChargeCurrency() {
        return TRY;
    }
}
