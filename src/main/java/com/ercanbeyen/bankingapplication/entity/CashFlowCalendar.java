package com.ercanbeyen.bankingapplication.entity;

import com.ercanbeyen.bankingapplication.embeddable.CashFlow;
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
    @ElementCollection
    @CollectionTable(
            name =  "calendar_cash_flows",
            joinColumns = @JoinColumn(
                    name = "calendar_id",
                    referencedColumnName = "id"
            )
    )
    private List<CashFlow> cashFlows;
}
