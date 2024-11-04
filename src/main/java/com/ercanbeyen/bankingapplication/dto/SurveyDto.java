package com.ercanbeyen.bankingapplication.dto;

import com.ercanbeyen.bankingapplication.embeddable.Rating;
import com.ercanbeyen.bankingapplication.entity.SurveyCompositeKey;

import java.time.LocalDateTime;
import java.util.List;

public record SurveyDto(
        SurveyCompositeKey key,
        String title,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<Rating> ratings,
        String customerSuggestion) {

}
