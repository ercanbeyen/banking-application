package com.ercanbeyen.bankingapplication.util;

import com.ercanbeyen.bankingapplication.dto.CustomerDto;
import com.ercanbeyen.bankingapplication.exception.BadRequestException;
import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@UtilityClass
public class CustomerUtil {
    private final long MINIMUM_AGE = 18;

    public void checkRequest(CustomerDto request) {
        if (ChronoUnit.YEARS.between(request.getBirthDate(), LocalDate.from(TimeUtil.getCurrentTimeStampInTurkey())) < MINIMUM_AGE) {
            throw new BadRequestException("Candidate customer is not an adult");
        }

        request.getAddresses().forEach(AddressUtil::checkAddressRequest);
    }
}
