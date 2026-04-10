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
    TERM_DEPOSIT_INTEREST_RATE("Term Deposit Interest Rate"),
    DEDUCTION("Deduction"),
    AGREEMENT("Agreement"),
    DAILY_ACTIVITY_LIMIT("Daily Activity Limit"),
    CASH_FLOW_CALENDAR("Cash Flow Calendar"),
    USER_CREDENTIALS("User Credentials"),
    ROLE("Role"),
    REFRESH_TOKEN("Refresh Token");

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
            case TERM_DEPOSIT_INTEREST_RATE -> "term-deposit-interest-rate";
            case USER_CREDENTIALS, ROLE, REFRESH_TOKEN -> throw new ResourceNotFoundException("Resource is not found");
            default -> value.toLowerCase();
        };
    }
}
