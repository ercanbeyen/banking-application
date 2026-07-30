package com.ercanbeyen.bankingapplication.entity;

import com.ercanbeyen.bankingapplication.constant.enums.SurveyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@PrimaryKeyClass
public class SurveyCompositeKey {
    @NotBlank(message = "National identity should not be blank")
    @Pattern(regexp = "\\d{11}", message = "Length of national identity must be 11 characters")
    @PrimaryKeyColumn(ordinal = 0, type = PrimaryKeyType.PARTITIONED, ordering = Ordering.ASCENDING)
    @Column(value = "customer_national_id")
    private String customerNationalId;
    @PrimaryKeyColumn(ordinal = 1, type = PrimaryKeyType.PARTITIONED, ordering = Ordering.ASCENDING)
    @Column(value = "account_activity_id")
    private String accountActivityId;
    @PrimaryKeyColumn(ordinal = 1, type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING)
    @Column(value = "survey_type")
    private SurveyType surveyType;
}
