package com.ercanbeyen.bankingapplication.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SourceType;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "customers_agreements")
public class CustomerAgreement {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @ManyToOne
    @JoinColumn(name = "customer_national_id", referencedColumnName = "national_id")
    private Customer customer;
    @ManyToOne
    @JoinColumn(name = "agreement_id", referencedColumnName = "id")
    private Agreement agreement;
    @CreationTimestamp(source = SourceType.DB)
    private Instant approvedAt;

    @Override
    public String toString() {
        return "CustomerAgreement{" +
                "id='" + id + '\'' +
                ", customer=" + customer.getNationalId() +
                ", agreement=" + agreement.getId() +
                ", approvedAt=" + approvedAt +
                '}';
    }
}
