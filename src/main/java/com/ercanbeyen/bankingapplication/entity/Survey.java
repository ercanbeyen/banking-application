package com.ercanbeyen.bankingapplication.entity;

import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import com.ercanbeyen.bankingapplication.dto.AccountActivityDto;
import com.ercanbeyen.bankingapplication.dto.SurveyDto;
import com.ercanbeyen.bankingapplication.embeddable.Rating;
import com.ercanbeyen.bankingapplication.util.TimeUtil;
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
    @Column(value = "account-activity-type")
    private AccountActivityType accountActivityType;
    @Column(value = "valid_until")
    private LocalDateTime validUntil;
    @Column(value = "updated_at")
    private LocalDateTime updatedAt;
    @Column(value = "customer_suggestion")
    private String customerSuggestion;
    @Frozen
    private List<Rating> ratings;

    public static Survey valueOf(SurveyDto surveyDto, AccountActivityDto accountActivityDto) {
        LocalDateTime now = TimeUtil.getTurkeyDateTime();
        SurveyCompositeKey key = new SurveyCompositeKey(
                surveyDto.key().getCustomerNationalId(),
                accountActivityDto.id(),
                now,
                surveyDto.key().getSurveyType()
        );

        /* Rates are null before updated by the customer */
        surveyDto.ratings()
                .forEach(rating -> rating.setRate(null));

        return Survey.builder()
                .key(key)
                .title(surveyDto.title())
                .validUntil(surveyDto.validUntil())
                .updatedAt(now)
                .accountActivityType(accountActivityDto.type())
                .customerSuggestion(surveyDto.customerSuggestion())
                .ratings(surveyDto.ratings())
                .build();
    }
}
