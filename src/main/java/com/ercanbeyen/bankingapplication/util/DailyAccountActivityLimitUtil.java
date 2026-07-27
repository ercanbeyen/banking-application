package com.ercanbeyen.bankingapplication.util;

import com.ercanbeyen.bankingapplication.dto.DailyAccountActivityLimitDto;
import com.ercanbeyen.bankingapplication.exception.BadRequestException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class DailyAccountActivityLimitUtil {
    public void checkRequest(DailyAccountActivityLimitDto request) {
        if (request.lowerLimit() > request.upperLimit()) {
            log.error("Lower limit should be less than equal to upper limit");
            throw new BadRequestException("Invalid daily activity request!");
        }
    }
}
