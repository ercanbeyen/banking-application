package com.ercanbeyen.bankingapplication.dto;

import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import com.ercanbeyen.bankingapplication.embeddable.Rating;
import com.ercanbeyen.bankingapplication.entity.SurveyCompositeKey;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record SurveyDto(
        @NotNull(message = "Key should not be null")
        SurveyCompositeKey key,
        @NotBlank(message = "Title should not be blank")
        @NotNull(message = "Title should not be null")
        String title,
        AccountActivityType accountActivityType,
        LocalDateTime updatedAt,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime validUntil,
        List<@Valid Rating> ratings,
        String customerSuggestion) {

}
