package com.ercanbeyen.bankingapplication.embeddable;

import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Embeddable
public class CashFlow {
    private String accountActivityId;
    @Enumerated(EnumType.STRING)
    private AccountActivityType accountActivityType;
    private Double amount;
    private LocalDate date;
}
