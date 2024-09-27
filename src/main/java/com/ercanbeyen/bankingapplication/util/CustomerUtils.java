package com.ercanbeyen.bankingapplication.util;

import com.ercanbeyen.bankingapplication.dto.CustomerDto;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@UtilityClass
public class CustomerUtils {
    private final long MINIMUM_AGE = 18;

    public void checkRequest(CustomerDto request) {
        if (ChronoUnit.YEARS.between(request.getBirthDate(), LocalDate.from(LocalDateTime.now())) < MINIMUM_AGE) {
            throw new ResourceConflictException("Candidate customer is not an adult");
        }

        request.getAddresses()
                .forEach(AddressUtils::checkAddressRequest);
    }
}
