package com.ercanbeyen.bankingapplication.dto;

import com.ercanbeyen.bankingapplication.constant.enums.ActivityType;
import com.ercanbeyen.bankingapplication.constant.enums.BranchOrderStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record BranchOrderDto(
        String id,
        @NotBlank(message = "Branch name should not be blank")
        String branchName,
        @NotBlank(message = "Customer national id should not be blank")
        String customerNationalId,
        String accountActivityId,
        @NotNull(message = "Activity type should not be null")
        ActivityType activityType,
        BranchOrderStatus status,
        @NotBlank(message = "Content should not be blank")
        String content,
        @JsonFormat(
                shape = JsonFormat.Shape.STRING,
                pattern = "yyyy-MM-dd HH:mm:ss",
                timezone = "UTC"
        )
        Instant createdAt) {

}
