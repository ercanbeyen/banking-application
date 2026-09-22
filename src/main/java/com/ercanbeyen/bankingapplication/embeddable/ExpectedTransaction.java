package com.ercanbeyen.bankingapplication.embeddable;

import com.ercanbeyen.bankingapplication.constant.enums.ActivityType;
import jakarta.persistence.Embeddable;

import java.time.LocalDate;

@Embeddable
public record ExpectedTransaction(ActivityType activityType, Double amount, LocalDate date) {

}
