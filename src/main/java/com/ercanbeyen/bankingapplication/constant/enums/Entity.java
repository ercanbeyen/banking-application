package com.ercanbeyen.bankingapplication.constant.enums;

import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
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
    ATM("ATM"),
    BRANCH("Branch"),
    TERM_DEPOSIT_INTEREST_RATE("Term Deposit Interest Rate"),
    DEDUCTION("Deduction"),
    AGREEMENT("Agreement"),
    NEWS("News"),
    DAILY_ACTIVITY_LIMIT("Daily Activity Limit"),
    CASH_FLOW_CALENDAR("Cash Flow Calendar"),
    USER_CREDENTIALS("User Credentials"),
    ROLE("Role"),
    PERMISSION("Permission"),
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
            case BRANCH -> value.toLowerCase() + "e";
            case NEWS -> {
                final int length = value.length();
                yield value.toLowerCase().substring(0, length - 1);
            }
            case CASH_FLOW_CALENDAR, NOTIFICATION, USER_CREDENTIALS, ROLE, PERMISSION, REFRESH_TOKEN, INCORRECT_LOGIN_ATTEMPT -> throw new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, "Resource"));
            case TERM_DEPOSIT_INTEREST_RATE, DAILY_ACTIVITY_LIMIT, MONEY_TRANSFER_ORDER -> {
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
            default -> value.toLowerCase();
        };
    }
}
