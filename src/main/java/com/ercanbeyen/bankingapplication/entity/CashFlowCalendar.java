package com.ercanbeyen.bankingapplication.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Data
@Entity
@Table(name = "cash_flow_calendars")
public class CashFlowCalendar {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @OneToOne(mappedBy = "cashFlowCalendar")
    private Customer customer;
    @OneToMany(mappedBy = "cashFlowCalendar")
    private List<CashFlow> cashFlows;
}
