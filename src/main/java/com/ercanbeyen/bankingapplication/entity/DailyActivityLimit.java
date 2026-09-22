package com.ercanbeyen.bankingapplication.entity;

import com.ercanbeyen.bankingapplication.constant.enums.ActivityType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Data
@Entity
@Table(name = "daily_activity_limits")
public class DailyActivityLimit {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @Enumerated(EnumType.STRING)
    @Column(unique = true, nullable = false)
    private ActivityType activityType;
    @Column(nullable = false)
    private Double lowerLimit;
    @Column(nullable = false)
    private Double upperLimit;
    @UpdateTimestamp
    private Instant modifiedAt;
}
