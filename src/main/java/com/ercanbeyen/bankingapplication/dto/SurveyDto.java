package com.ercanbeyen.bankingapplication.dto;

import com.ercanbeyen.bankingapplication.embeddable.Rating;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SurveyDto(
        UUID id,
        @NotBlank(message = "National identity should not be blank")
        @Pattern(regexp = "\\d{11}", message = "Length of national identity must be 11 characters")
        String customerNationalId,
        String title,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Integer year,
        List<Rating> ratings,
        String customerSuggestion) {

}
