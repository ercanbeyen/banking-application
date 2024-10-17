package com.ercanbeyen.bankingapplication.constant.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Entity {
    CUSTOMER("Customer"),
    ACCOUNT("Account"),
    FILE("File"),
    NOTIFICATION("Notification"),
    ACCOUNT_ACTIVITY("Account activity"),
    TRANSFER_ORDER("Transfer order"),
    RATING("Rating"),
    EXCHANGE("Exchange"),
    ADDRESS("Address"),
    BRANCH("Branch"),
    CHARGE("Charge");

    private final String value;

    public String getCollectionUrl() {
        String resource = getResource();
        return "http://localhost:8080/api/v1/" + resource + "s";
    }

    private String getResource() {
        return switch (this) {
            case TRANSFER_ORDER -> "transfer-order";
            case ACCOUNT_ACTIVITY -> "account-activitie";
            case BRANCH -> "branche";
            default -> value.toLowerCase();
        };
    }
}
