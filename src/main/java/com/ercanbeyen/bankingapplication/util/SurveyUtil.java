package com.ercanbeyen.bankingapplication.util;

import com.ercanbeyen.bankingapplication.dto.SurveyDto;
import com.ercanbeyen.bankingapplication.exception.BadRequestException;
import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;

@UtilityClass
public class SurveyUtil {
    public void checkRequestBeforeSave(SurveyDto request) {
        if (request.validUntil().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Validation time should not be before now");
        }
    }

    public LocalDateTime getNearestValidationTime() {
        return LocalDateTime.now().plusHours(1);
    }

    public void checkStatisticsParameters(LocalDateTime createdAt, Integer frequency) {
        if (createdAt.isAfter(LocalDateTime.now())) {
            throw new BadRequestException("Created at value should not be after now");
        }

        final int minimumValue = 0;

        if (frequency < minimumValue) {
            throw new BadRequestException("Minimum value of frequency should be " + minimumValue);
        }
    }
}
