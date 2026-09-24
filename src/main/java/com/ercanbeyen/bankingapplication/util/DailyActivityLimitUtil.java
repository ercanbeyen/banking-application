package com.ercanbeyen.bankingapplication.util;

import com.ercanbeyen.bankingapplication.dto.DailyActivityLimitDto;
import com.ercanbeyen.bankingapplication.exception.BadRequestException;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DailyActivityLimitUtil {
    public void checkRequest(DailyActivityLimitDto request) {
        if (request.lowerLimit() > request.upperLimit()) {
            throw new BadRequestException("Upper limit should not be less than lower limit!");
        }
    }
}
