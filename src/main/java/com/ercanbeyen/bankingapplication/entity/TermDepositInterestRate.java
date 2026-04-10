package com.ercanbeyen.bankingapplication.entity;

import com.ercanbeyen.bankingapplication.constant.enums.Currency;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "term_deposit_interest_rates")
public non-sealed class TermDepositInterestRate extends BaseEntity {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Currency currency;
    @Column(nullable = false)
    private Double minimumBalance;
    @Column(nullable = false)
    private Double maximumBalance;
    @Column(nullable = false)
    private Integer depositMaturity;
    @Column(nullable = false)
    private Double interestRate;
}
