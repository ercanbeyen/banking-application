package com.ercanbeyen.bankingapplication.entity;

import com.ercanbeyen.bankingapplication.embeddable.Rating;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.cassandra.core.mapping.Frozen;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Table(value = "surveys")
public class Survey {
    @PrimaryKey
    private UUID id;
    @Column(name = "customer_national_id")
    private String customerNationalId;
    private String title;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer year;
    @Frozen
    private List<Rating> ratings;
    private String customerSuggestion;
}
