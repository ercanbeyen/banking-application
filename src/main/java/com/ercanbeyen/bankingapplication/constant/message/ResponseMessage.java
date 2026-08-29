package com.ercanbeyen.bankingapplication.constant.message;

import com.ercanbeyen.bankingapplication.util.AuthUtil;
import com.ercanbeyen.bankingapplication.util.PhotoUtil;

public final class ResponseMessage {
    public static final String NOT_FOUND = "%s is not found";
    public static final String ALREADY_EXISTS = "%s already exists";
    public static final String SUCCESS = "%s is successfully completed";
    public static final String DELETE_SUCCESS = "%s is successfully deleted";
    public static final String FILE_UPLOAD_APPROVAL = "The file has been uploaded to the system. It is being processed in the background...";
    public static final String FILES_UPLOAD_APPROVAL = "The files have been uploaded to the system. They are being processed in the background...";
    public static final String EMAIL_SENT_SUCCESS = "Email is successfully sent!";
    public static final String INVALID_CONTENT_TYPE = "Invalid content type";
    public static final String INVALID_PHOTO_CONTENT_TYPE = INVALID_CONTENT_TYPE + ". Valid content types for photo are " + PhotoUtil.getPlainContentTypes();
    public static final String EVALUATION_MESSAGE = "Please evaluate your %s activity at %s in the %s before %s";
    public static final String INVALID_PHONE_NUMBER = "Invalid phone number";
    public static final String IMPROPER_ACCOUNT = "Account is improper for activities";
    public static final String IMPROPER_ACCOUNT_ACTIVITY = "Account activity is improper";
    public static final String UNPAIRED_CURRENCIES = "Currencies must be %s";
    public static final String INSUFFICIENT_FUNDS = "Insufficient funds";
    public static final String TRANSACTION_FEE_CANNOT_BE_PAYED = "Transaction fee cannot be payed";
    public static final String INVALID_DEDUCTEE_ACCOUNT_CURRENCY = "Deductee %s's currency should be %s";
    public static final String PASSWORD_SHOULD_BE_DIFFERENT = "New password should be different from your last " + AuthUtil.getPasswordHistoryMaxSize() + " passwords!";
    public static final String IMPROPER_DEDUCTEE_ACCOUNT = "Deductee %s should not be indicated for %s account activities";
    public static final String UNACCEPTABLE_CHANNEL = "Unacceptable channel!";
    public static final String ACCESS_DENIED = "{\"error\": \"Access Denied - %s\"}";

    private ResponseMessage() {}
}
