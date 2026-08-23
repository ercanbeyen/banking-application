package com.ercanbeyen.bankingapplication.dto.response;

import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;

import java.time.LocalDateTime;

public record ReceiptPreview(String id, AccountActivityType activityType, LocalDateTime time, Double amount) {

}
