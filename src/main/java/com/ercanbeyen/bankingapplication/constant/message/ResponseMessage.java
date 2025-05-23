package com.ercanbeyen.bankingapplication.constant.message;

import com.ercanbeyen.bankingapplication.util.PhotoUtil;

public final class ResponseMessage {
    public static final String NOT_FOUND = "%s is not found";
    public static final String ALREADY_EXISTS = "%s already exists";
    public static final String SUCCESS = "%s is successfully completed";
    public static final String FILE_UPLOAD_SUCCESS = "File is successfully uploaded";
    public static final String FILE_DELETE_SUCCESS = "File is successfully deleted";
    public static final String FILE_UPLOAD_ERROR = "Error occurred while uploading file";
    public static final String INVALID_CONTENT_TYPE = "Invalid content type";
    public static final String INVALID_PHOTO_CONTENT_TYPE = INVALID_CONTENT_TYPE + ". Valid content types for photo are " + PhotoUtil.getPlainContentTypes();
    public static final String INVALID_PHONE_NUMBER = "Invalid phone number";
    public static final String IMPROPER_ACCOUNT = "Account is improper for activities";
    public static final String IMPROPER_ACCOUNT_ACTIVITY = "Account activity is improper";
    public static final String UNPAIRED_CURRENCIES = "Currencies must be %s";
    public static final String INSUFFICIENT_FUNDS = "Insufficient funds";
    public static final String TRANSACTION_FEE_CANNOT_BE_PAYED = "Transaction fee cannot be payed";

    private ResponseMessage() {}
}
