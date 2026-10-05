package com.ercanbeyen.bankingapplication.entity;

import com.ercanbeyen.bankingapplication.constant.enums.ActivityType;
import com.ercanbeyen.bankingapplication.constant.enums.BranchOrderStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Data
@Entity
@Table(name = "branch_orders")
public class BranchOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @Enumerated(EnumType.STRING)
    private ActivityType activityType;
    @Enumerated(EnumType.STRING)
    private BranchOrderStatus status;
    @ManyToOne
    private Customer customer;
    @ManyToOne
    private Branch branch;
    private String content;
    @CreationTimestamp
    private Instant createdAt;
    @UpdateTimestamp
    private Instant updatedAt;
}
