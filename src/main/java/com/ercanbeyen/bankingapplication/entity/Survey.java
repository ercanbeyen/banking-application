package com.ercanbeyen.bankingapplication.entity;

import com.ercanbeyen.bankingapplication.dto.SurveyDto;
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
    @Column(value = "valid_until")
    private LocalDateTime validUntil;
    @Column(value = "updated_at")
    private LocalDateTime updatedAt;
    @Column(value = "customer_suggestion")
    private String customerSuggestion;
    @Frozen
    private List<Rating> ratings;

    public static Survey valueOf(SurveyDto request) {
        LocalDateTime now = LocalDateTime.now();
        SurveyCompositeKey key = new SurveyCompositeKey(
                request.key().getCustomerNationalId(),
                request.key().getAccountActivityId(),
                now,
                request.key().getSurveyType()
        );

        return Survey.builder()
                .key(key)
                .title(request.title())
                .validUntil(request.validUntil())
                .updatedAt(now)
                .customerSuggestion(request.customerSuggestion())
                .ratings(request.ratings())
                .build();
    }
}
