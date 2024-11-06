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

import java.time.LocalDate;
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
    private LocalDate validUntil;
    @Column(value = "updated_at")
    private LocalDate updatedAt;
    @Frozen
    private List<Rating> ratings;
    @Column(value = "customer_suggestion")
    private String customerSuggestion;

    public static Survey valueOf(SurveyDto request) {
        LocalDate now = LocalDate.now();
        SurveyCompositeKey requestedKey = request.key();

        requestedKey = new SurveyCompositeKey(requestedKey.getCustomerNationalId(), requestedKey.getSurveyType(), now);

        return Survey.builder()
                .key(requestedKey)
                .title(request.title())
                .validUntil(request.validUntil())
                .updatedAt(now)
                .customerSuggestion(request.customerSuggestion())
                .ratings(request.ratings())
                .build();
    }
}
