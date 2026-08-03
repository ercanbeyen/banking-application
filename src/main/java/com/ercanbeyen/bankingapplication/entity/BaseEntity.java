package com.ercanbeyen.bankingapplication.entity;


import com.ercanbeyen.bankingapplication.listener.BaseEntityListener;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@EntityListeners(BaseEntityListener.class)
@Data
@MappedSuperclass
public abstract sealed class BaseEntity permits Account, Atm, Branch, Customer, Exchange, TermDepositInterestRate, News, NewsReport, MoneyTransferOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @CreationTimestamp
    private Instant createdAt;
    @UpdateTimestamp
    private Instant updatedAt;
}
