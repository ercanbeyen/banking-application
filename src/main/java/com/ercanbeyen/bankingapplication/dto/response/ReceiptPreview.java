package com.ercanbeyen.bankingapplication.dto.response;

import com.ercanbeyen.bankingapplication.constant.enums.ActivityType;

import java.time.LocalDateTime;

public record ReceiptPreview(String id, ActivityType activityType, LocalDateTime time, Double amount) {

}
