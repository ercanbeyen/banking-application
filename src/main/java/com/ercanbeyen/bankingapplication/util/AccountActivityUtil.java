package com.ercanbeyen.bankingapplication.util;

import com.ercanbeyen.bankingapplication.dto.request.AccountActivityFilteringRequest;
import com.ercanbeyen.bankingapplication.exception.BadRequestException;
import com.ercanbeyen.bankingapplication.option.AccountActivityFilteringOption;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.Optional;
import java.util.function.Predicate;

@Slf4j
@UtilityClass
public class AccountActivityUtil {
    public void checkFilteringOption(AccountActivityFilteringOption filteringOption) {
        checkDates(filteringOption.fromDate(), filteringOption.toDate());
    }

    public void checkFilteringRequest(AccountActivityFilteringRequest request) {
        checkDates(request.fromDate(), request.toDate());
    }

    private void checkDates(LocalDate fromDate, LocalDate toDate) {
        if (isDateEmpty.test(fromDate)) {
            log.warn("From date is null");
            return;
        }

        if (isDateEmpty.test(toDate)) {
            log.warn("To date is null");
            return;
        }

        if (toDate.isBefore(fromDate)) {
            throw new BadRequestException("To date cannot be before from date");
        }

        log.info("Dates are compatible");
    }

    private final Predicate<LocalDate> isDateEmpty = localDate -> Optional.ofNullable(localDate).isEmpty();
}
