package com.ercanbeyen.bankingapplication.model;

import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import com.ercanbeyen.bankingapplication.embeddable.Rating;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.Frozen;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(value = "surveys")
public class Survey {
    @PrimaryKey
    private SurveyCompositeKey key;
    @Column(value = "title")
    private String title;
    @Column(value = "account_activity_type")
    private AccountActivityType accountActivityType;
    @Column(value = "valid_until")
    private LocalDateTime validUntil;
    @Column(value = "updated_at")
    private LocalDateTime updatedAt;
    @Column(value = "customer_suggestion")
    private String customerSuggestion;
    @Frozen
    private List<Rating> ratings;
}
