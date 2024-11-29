package com.ercanbeyen.bankingapplication.entity;

import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import com.ercanbeyen.bankingapplication.embeddable.CashFlowPK;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
@Table(name = "cash_flows")
public class CashFlow {
    @EmbeddedId
    private CashFlowPK cashFlowPK;
    @ManyToOne
    private CashFlowCalendar cashFlowCalendar;
    @Enumerated(EnumType.STRING)
    private AccountActivityType accountActivityType;
    private Double amount;
    private LocalDate date;
}
