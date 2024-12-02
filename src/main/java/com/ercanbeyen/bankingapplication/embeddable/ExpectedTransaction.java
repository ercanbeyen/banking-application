package com.ercanbeyen.bankingapplication.embeddable;

import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import jakarta.persistence.Embeddable;

import java.time.LocalDate;

@Embeddable
public record ExpectedTransaction(AccountActivityType accountActivityType, Double amount, LocalDate date) {

}
