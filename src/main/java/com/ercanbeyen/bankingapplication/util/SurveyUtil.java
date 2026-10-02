package com.ercanbeyen.bankingapplication.util;

import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.dto.SurveyDto;
import com.ercanbeyen.bankingapplication.exception.BadRequestException;
import lombok.experimental.UtilityClass;

import java.time.Instant;
import java.util.Optional;

@UtilityClass
public class SurveyUtil {
    private final int MINIMUM_FREQUENCY = 0;
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

    public void checkStatisticsParameters(Integer frequency) {
        if (frequency < MINIMUM_FREQUENCY) {
            throw new BadRequestException("Minimum frequency must be at least " + MINIMUM_FREQUENCY);
        }
    }
}
