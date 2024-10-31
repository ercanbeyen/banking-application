package com.ercanbeyen.bankingapplication.constant.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.*;

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
    private static final Set<AccountActivityType> accountStatusUpdatingActivities;
    private static final Map<AccountActivityType, Double> maximumAmountsPerRequest;

    static {
        accountStatusUpdatingActivities = EnumSet.of(ACCOUNT_OPENING, ACCOUNT_BLOCKING, ACCOUNT_CLOSING);
        maximumAmountsPerRequest = new EnumMap<>(AccountActivityType.class);
        maximumAmountsPerRequest.put(MONEY_TRANSFER, 1_000_000D);
        maximumAmountsPerRequest.put(MONEY_EXCHANGE, 100_000D);
    }

    public static Double getMaximumAmountPerRequestOfActivity(AccountActivityType activityType) {
        return maximumAmountsPerRequest.get(activityType);
    }
}
