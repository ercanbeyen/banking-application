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
    REGULAR_TRANSFER_ORDER("Regular transfer order"),
    RATING("Rating"),
    EXCHANGE("Exchange"),
    ADDRESS("Address");

    private final String value;

    public String getCollectionUrl() {
        String resource = getResource();
        return "http://localhost:8080/api/v1/" + resource + "s";
    }

    private String getResource() {
        return switch (this) {
            case REGULAR_TRANSFER_ORDER -> "regular-transfer-order";
            case ACCOUNT_ACTIVITY -> "account-activitie";
            default -> value.toLowerCase();
        };
    }
}
