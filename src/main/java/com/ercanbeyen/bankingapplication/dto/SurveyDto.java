package com.ercanbeyen.bankingapplication.dto;

import com.ercanbeyen.bankingapplication.embeddable.Rating;
import com.ercanbeyen.bankingapplication.entity.SurveyCompositeKey;

import java.time.LocalDate;
import java.util.List;

public record SurveyDto(
        SurveyCompositeKey key,
        String title,
        LocalDate updatedAt,
        LocalDate validUntil,
        List<Rating> ratings,
        String customerSuggestion) {

}
