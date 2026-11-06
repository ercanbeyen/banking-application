package com.ercanbeyen.bankingapplication.util;

import lombok.experimental.UtilityClass;

import java.time.LocalDate;

@UtilityClass
public class AccountStatementUtil {
    public final String CUSTOMER_FULL_NAME = "Customer Full Name: ";
    public final String CUSTOMER_NUMBER = "Customer Number: ";
    public final String CUSTOMER_NATIONAL_IDENTITY_NUMBER = "Customer National Identity Number: ";
    public final String BRANCH = "Branch: ";
    public final String ACCOUNT_IDENTITY = "Account Identity: ";
    public final String ACCOUNT_TYPE = "Account Type: ";
    public final String ACCOUNT_CURRENCY = "Account Currency: ";
    public final String BALANCE = "Balance: ";
    public final String DOCUMENT_ISSUE_DATE = "Document Issue Date: ";
    public final String INQUIRY_CRITERIA = "Inquiry Criteria: ";

    public String writeInquiryCriteria(LocalDate fromDate, LocalDate toDate) {
        return fromDate + " - " + toDate;
    }
}
