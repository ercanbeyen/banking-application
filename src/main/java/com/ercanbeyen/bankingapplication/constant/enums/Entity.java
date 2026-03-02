package com.ercanbeyen.bankingapplication.constant.enums;

import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Entity {
    CUSTOMER("Customer"),
    ACCOUNT("Account"),
    FILE("File"),
    NOTIFICATION("Notification"),
    ACCOUNT_ACTIVITY("Account Activity"),
    MONEY_TRANSFER_ORDER("Money Transfer Order"),
    SURVEY("Survey"),
    EXCHANGE("Exchange"),
    BRANCH("Branch"),
    FEE("Fee"),
    CHARGE("Charge"),
    AGREEMENT("Agreement"),
    DAILY_ACTIVITY_LIMIT("Daily Activity Limit"),
    CASH_FLOW_CALENDAR("Cash Flow Calendar"),
    USER_CREDENTIAL("User Credential"),
    ROLE("Role");

    private final String value;

    public String getCollectionUrl() {
        String resource = getResource();
        return "http://localhost:8080/api/v1/" + resource + "s";
    }

    private String getResource() {
        return switch (this) {
            case MONEY_TRANSFER_ORDER -> "money-transfer-order";
            case ACCOUNT_ACTIVITY -> "account-activitie";
            case BRANCH -> "branche";
            case USER_CREDENTIAL, ROLE -> throw new ResourceNotFoundException("Resource is not found");
            default -> value.toLowerCase();
        };
    }
}
