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
    INTEREST_INCOME("Interest Income"),
    DEDUCTION("Deduction"),
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

    public static Double getMaximumAmountPerRequestOfActivity(AccountActivityType accountActivityType) {
        return maximumAmountsPerRequest.get(accountActivityType);
    }

    public static ChannelType getChannelTypeWithNoDailyAccountActivityLimit() {
        return ChannelType.BRANCH;
    }

    public List<ChannelType> getAvailableChannelTypes() {
        return switch (this) {
            case MONEY_DEPOSIT, WITHDRAWAL -> List.of(ChannelType.BRANCH, ChannelType.ATM);
            case MONEY_TRANSFER, MONEY_EXCHANGE ->
                    List.of(ChannelType.BRANCH, ChannelType.ATM, ChannelType.INTERNET_BANKING, ChannelType.MOBILE_BANKING);
            case ACCOUNT_OPENING, ACCOUNT_BLOCKING, ACCOUNT_CLOSING, INTEREST_INCOME, DEDUCTION ->
                    List.of(ChannelType.SYSTEM);
        };
    }
}
