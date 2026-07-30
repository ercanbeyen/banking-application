package com.ercanbeyen.bankingapplication.entity;

import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import com.ercanbeyen.bankingapplication.constant.enums.Channel;
import com.ercanbeyen.bankingapplication.embeddable.Rating;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.mapping.*;

import java.time.Instant;
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
    @Column(value = "channel")
    private Channel channel;
    @Column(value = "valid_until")
    private Instant validUntil;
    @Column(value = "filled_out_at")
    private Instant filledOutAt;
    @Column(value = "created_at")
    private Instant createdAt;
    @Column(value = "updated_at")
    private Instant updatedAt;
    @Column(value = "customer_suggestion")
    private String customerSuggestion;
    @Frozen
    private List<Rating> ratings;
}
