package com.ercanbeyen.bankingapplication.entity;

import com.ercanbeyen.bankingapplication.constant.query.Query;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "customer_agreement")
public class CustomerAgreement {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @ManyToOne
    @JoinColumn(name = "customer_national_id", referencedColumnName = "national_id")
    private Customer customer;
    @ManyToOne
    @JoinColumn(name = "agreement_title", referencedColumnName = "title")
    private Agreement agreement;
    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "approved_at", columnDefinition = Query.TIMESTAMP_DEFAULT_NOW)
    private LocalDateTime approvedAt;

    @Override
    public String toString() {
        return "CustomerAgreement{" +
                "id='" + id + '\'' +
                ", customer=" + customer.getNationalId() +
                ", agreement=" + agreement.getTitle() +
                ", approvedAt=" + approvedAt +
                '}';
    }
}
