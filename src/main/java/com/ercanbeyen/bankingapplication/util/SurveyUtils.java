package com.ercanbeyen.bankingapplication.util;

import com.ercanbeyen.bankingapplication.dto.SurveyDto;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;

@UtilityClass
public class SurveyUtils {
    public void checkRequestBeforeSave(SurveyDto request) {
        if (request.validUntil().isBefore(LocalDateTime.now())) {
            throw new ResourceConflictException("Validation date should not be before today");
        }
    }
}
