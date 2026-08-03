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
    NEWS("News"),
    SURVEY("Survey"),
    EXCHANGE("Exchange"),
    ATM("Atm"),
    BRANCH("Branch"),
    TERM_DEPOSIT_INTEREST_RATE("Term Deposit Interest Rate"),
    DEDUCTION("Deduction"),
    AGREEMENT("Agreement"),
    DAILY_ACCOUNT_ACTIVITY_LIMIT("Daily Account Activity Limit"),
    CASH_FLOW_CALENDAR("Cash Flow Calendar"),
    USER_CREDENTIALS("User Credentials"),
    ROLE("Role"),
    REFRESH_TOKEN("Refresh Token"),
    INCORRECT_LOGIN_ATTEMPT("Incorrect Login Attempt");

    private final String value;

    public String getCollectionUrl() {
        String resource = getResource();
        return "http://localhost:8080/api/v1/" + resource + "s";
    }

    private String getResource() {
        return switch (this) {
            case ACCOUNT_ACTIVITY -> "account-activitie";
            case BRANCH -> "branche";
            case NEWS -> "new";
            case CASH_FLOW_CALENDAR, USER_CREDENTIALS, ROLE, REFRESH_TOKEN, INCORRECT_LOGIN_ATTEMPT -> throw new ResourceNotFoundException("Resource is not found");
            default -> {
                String[] words = value.toLowerCase().split(" ");
                StringBuilder stringBuilder = new StringBuilder();

                for (int i = 0; i < words.length; i++) {
                    String word = words[i];
                    stringBuilder.append(word);

                    if (i != words.length - 1) {
                        stringBuilder.append("-");
                    }
                }

                yield stringBuilder.toString();
            }
        };
    }
}
