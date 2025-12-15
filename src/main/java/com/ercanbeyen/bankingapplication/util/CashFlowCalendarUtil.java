package com.ercanbeyen.bankingapplication.util;

import com.ercanbeyen.bankingapplication.exception.BadRequestException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;

@Slf4j
@UtilityClass
public class CashFlowCalendarUtil {
    private final Set<Integer> VALID_MONTHS = Set.of(1, 2, 3, 4);

    public void checkMonthValueForExpectedTransactions(Integer month) {
        if (!VALID_MONTHS.contains(month)) {
            throw new BadRequestException("Month is invalid. Valid values are " + VALID_MONTHS);
        }
    }

    public void checkMonthAndYearForCashFlowCalendar(Integer year, Integer month) {
        LocalDate today = LocalDate.now();
        int mostDistantMonth = Collections.max(VALID_MONTHS);
        LocalDate mostDistantFuture = today.plusMonths(mostDistantMonth);

        if (isDateFuture(today, year, month)) {
            log.info("Past cash flows are requested. So, no need to check year and month");
            return;
        }

        if (isDatePast(today, year, month) && mostDistantFuture.getMonthValue() < month) {
            throw new BadRequestException(String.format("At most %d most further can be displayed", mostDistantMonth));
        }

        log.info("Year and month values are valid for requesting cash flows");
    }

    public boolean isDateFuture(LocalDate date, Integer year, Integer month) {
        return (date.getYear() > year) || (date.getYear() == year && date.getMonthValue() > month);
    }

    public boolean isDatePast(LocalDate date, Integer year, Integer month) {
        return (date.getYear() < year) || (date.getYear() == year && date.getMonthValue() > month);
    }

    public boolean isDateThisMonth(LocalDate date, Integer year, Integer month) {
        return (date.getYear() == year) && (date.getMonthValue() == month);
    }
}
