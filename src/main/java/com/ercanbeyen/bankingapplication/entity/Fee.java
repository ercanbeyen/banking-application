package com.ercanbeyen.bankingapplication.entity;

import com.ercanbeyen.bankingapplication.constant.enums.Currency;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "fees")
public non-sealed class Fee extends BaseEntity {
    @Column(nullable = false)
    private Currency currency;
    @Column(nullable = false)
    private Double minimumAmount;
    @Column(nullable = false)
    private Double maximumAmount;
    @Column(nullable = false)
    private Integer depositPeriod;
    @Column(nullable = false)
    private Double interestRatio;
}
