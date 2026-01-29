package com.ercanbeyen.bankingapplication.util;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class AccountStatementUtil {
    public static final String CUSTOMER_NUMBER = "Customer Number: ";
    public static final String CUSTOMER_NATIONAL_IDENTITY_NUMBER = "Customer National Identity Number: ";
    public static final String BRANCH = "Branch: ";
    public static final String ACCOUNT_IDENTITY = "Account Identity: ";
    public static final String ACCOUNT_TYPE = "Account Type: ";
    public static final String ACCOUNT_CURRENCY = "Account Currency: ";
    public static final String BALANCE = "Balance: ";
    public static final String DOCUMENT_ISSUE_DATE = "Document Issue Date: ";
    public static final String INQUIRY_CRITERIA = "Inquiry Criteria: ";

    public static String writeFullName(String fullName) {
        return "Dear " + fullName;
    }

    public static String writeDocumentIssueDate(LocalDateTime localDateTime) {
        return localDateTime.toLocalDate() + " " + TimeUtil.getTimeStatement(localDateTime.toLocalTime());
    }

    public static String writeInquiryCriteria(LocalDate fromDate, LocalDate toDate) {
        return fromDate + " - " + toDate;
    }

    private AccountStatementUtil() {}
}
