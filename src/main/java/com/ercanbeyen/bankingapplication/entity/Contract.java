package com.ercanbeyen.bankingapplication.entity;

import com.ercanbeyen.bankingapplication.constant.query.Queries;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "contracts")
public class Contract {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String name;
    @ManyToOne
    @JoinColumn(name = "file_id", referencedColumnName = "id")
    private File file;
    @ManyToOne
    @JoinColumn(name = "customer_national_id", referencedColumnName = "national_id")
    private Customer customer;
    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "approved_at", columnDefinition = Queries.GET_NOW_TIMESTAMP)
    LocalDateTime approvedAt;
}
