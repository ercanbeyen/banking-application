package com.ercanbeyen.bankingapplication.entity;

import com.ercanbeyen.bankingapplication.constant.enums.AgreementSubject;
import com.ercanbeyen.bankingapplication.constant.query.Query;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Entity
@Table(name = "agreements")
public class Agreement {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @Column(unique = true, nullable = false)
    private String title;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgreementSubject subject;
    @ManyToOne
    @JoinColumn(name = "file_id", referencedColumnName = "id")
    private File file;
    @ManyToMany
    @JoinTable(
            name = "agreement_customer",
            joinColumns = @JoinColumn(
                    name = "agreement_title",
                    referencedColumnName = "title"
            ),
            inverseJoinColumns = @JoinColumn(
                    name = "customer_national_id",
                    referencedColumnName = "national_id"
            )
    )
    private Set<Customer> customers;
    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", columnDefinition = Query.TIMESTAMP_DEFAULT_NOW)
    LocalDateTime createdAt;
}
