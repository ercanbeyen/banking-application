package com.ercanbeyen.bankingapplication.constant.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.EnumMap;
import java.util.Map;

@RequiredArgsConstructor
public enum AccountActivityType {
    MONEY_DEPOSIT("Money Deposit"),
    WITHDRAWAL("Withdrawal"),
    MONEY_TRANSFER("Money Transfer"),
    MONEY_EXCHANGE("Money Exchange"),
    FEE("Fee"),
    CHARGE("Charge"),
    ACCOUNT_OPENING("Account Opening"),
    ACCOUNT_BLOCKING("Account Blocking"),
    ACCOUNT_CLOSING("Account Closing");

    @Getter
    private final String value;
    @Getter
    private static final Map<AccountActivityType, Double> activityToLimits;

    static {
        activityToLimits = new EnumMap<>(AccountActivityType.class);
        activityToLimits.put(MONEY_DEPOSIT, 10_000D);
        activityToLimits.put(WITHDRAWAL, 5_000D);
        activityToLimits.put(MONEY_TRANSFER, 1_000_000D);
        activityToLimits.put(MONEY_EXCHANGE, 5_000D);
    }
}
