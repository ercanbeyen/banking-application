package com.ercanbeyen.bankingapplication.util;

import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;
import java.time.Month;

@UtilityClass
public class SurveyUtils {
    private static final Month START_MONTH = Month.SEPTEMBER;

    public void checkSurveyBeforeSave() {
        LocalDateTime now = LocalDateTime.now();
        int currentYear = now.getYear();

        if (START_MONTH.getValue() > now.getMonth().getValue()) {
            String message = String.format("%d surveys start in %s and ends in the end of the year", currentYear, START_MONTH);
            throw new ResourceConflictException(message);
        }
    }
}
