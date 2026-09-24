package com.ercanbeyen.bankingapplication.entity;

import com.ercanbeyen.bankingapplication.constant.enums.ActivityType;
import com.ercanbeyen.bankingapplication.constant.enums.ChannelType;
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
    @Column(value = "activity_type")
    private ActivityType activityType;
    @Column(value = "channel_type")
    private ChannelType channelType;
    @Column(value = "valid_until")
    private Instant validUntil;
    @Column(value = "created_at")
    private Instant createdAt;
    @Column(value = "updated_at")
    private Instant updatedAt;
    @Column(value = "customer_suggestion")
    private String customerSuggestion;
    @Frozen
    private List<Rating> ratings;
}
