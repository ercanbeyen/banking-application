package com.ercanbeyen.bankingapplication.entity;

import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.SourceType;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "deductions")
public class Deduction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @Enumerated(EnumType.STRING)
    @Column(unique = true, nullable = false)
    private AccountActivityType activityType;
    @Column(nullable = false)
    private Double amount;
    @UpdateTimestamp(source = SourceType.DB)
    private LocalDateTime modifiedAt;
}
