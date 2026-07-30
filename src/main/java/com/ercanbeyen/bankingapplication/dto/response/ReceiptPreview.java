package com.ercanbeyen.bankingapplication.dto.response;

import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;

import java.time.Instant;

public record ReceiptPreview(String id, AccountActivityType activityType, Instant time, Double amount) {

}
