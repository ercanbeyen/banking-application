package com.ercanbeyen.bankingapplication.util;

import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.dto.SurveyDto;
import com.ercanbeyen.bankingapplication.exception.BadRequestException;
import lombok.experimental.UtilityClass;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

@UtilityClass
public class SurveyUtil {
    private final int AT_LEAST_VALIDATION_HOUR = 1;

    public void checkRequestBeforeSave(SurveyDto request) {
        if (request.validUntil().isBefore(Instant.now().plusSeconds(60L * AT_LEAST_VALIDATION_HOUR))) {
            throw new BadRequestException(Entity.SURVEY.getValue() + " must be valid for at least " + AT_LEAST_VALIDATION_HOUR + " hour");
        }
    }

    public void checkEvaluation(SurveyDto evaluation) {
        evaluation.ratings().forEach(rating -> {
            if (Optional.ofNullable(rating.getRate()).isEmpty()) {
                throw new BadRequestException("Rates should not be null in the evaluation");
            }
        });
    }

    public void checkStatisticsParameters(LocalDate createdDate, Integer frequency) {
        if (createdDate.isAfter(LocalDate.ofInstant(Instant.now(), ZoneId.systemDefault()))) {
            throw new BadRequestException("Value of created at should not be after now");
        }

        final int minimumValue = 0;

        if (frequency < minimumValue) {
            throw new BadRequestException("Minimum value of frequency should be " + minimumValue);
        }
    }
}
