package com.ercanbeyen.bankingapplication.entity;

import com.ercanbeyen.bankingapplication.constant.enums.AccountType;
import com.ercanbeyen.bankingapplication.constant.enums.Currency;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
@Entity
@Table(name = "accounts")
@NamedStoredProcedureQuery(
        name = "getTotalAccountsByCityAndTypeAndCurrency",
        procedureName = "get_total_accounts_by_city_and_type_and_currency",
        parameters = {
                @StoredProcedureParameter(mode = ParameterMode.IN, name = "city", type = String.class),
                @StoredProcedureParameter(mode = ParameterMode.IN, name = "type", type = String.class),
                @StoredProcedureParameter(mode = ParameterMode.IN, name = "currency", type = String.class),
                @StoredProcedureParameter(mode = ParameterMode.OUT, name = "count_of_accounts", type = Integer.class)
        })
public non-sealed class Account extends BaseEntity {
    @Enumerated(EnumType.STRING)
    private AccountType type;
    @ManyToOne
    @JoinColumn(name = "customer_national_id", referencedColumnName = "national_id")
    private Customer customer;
    @ManyToOne
    @JoinColumn(name = "branch_id", referencedColumnName = "id")
    private Branch branch;
    @Enumerated(EnumType.STRING)
    private Currency currency;
    private double balance;
    private boolean isBlocked;
    private LocalDateTime closedAt;
    @OneToMany(mappedBy = "senderAccount", cascade = CascadeType.ALL)
    private List<MoneyTransferOrder> moneyTransferOrders = new ArrayList<>();
    /* Deposit Account fields */
    private Double interestRatio;
    private Integer depositPeriod;
    private Double balanceAfterNextFee;
}
