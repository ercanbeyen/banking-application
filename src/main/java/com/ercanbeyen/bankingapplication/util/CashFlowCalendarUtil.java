package com.ercanbeyen.bankingapplication.util;

import com.ercanbeyen.bankingapplication.exception.BadRequestException;
import lombok.experimental.UtilityClass;

import java.util.Set;

@UtilityClass
public class CashFlowCalendarUtil {
    private final Set<Integer> VALID_MONTHS = Set.of(1, 2, 3, 4);

    public void checkMonthValueForExpectedTransactions(Integer month) {
        if (!VALID_MONTHS.contains(month)) {
            throw new BadRequestException("Month is invalid. Valid values are " + VALID_MONTHS);
        }
    }
}
