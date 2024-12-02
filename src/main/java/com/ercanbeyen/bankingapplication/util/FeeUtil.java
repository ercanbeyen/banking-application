package com.ercanbeyen.bankingapplication.util;

import com.ercanbeyen.bankingapplication.dto.FeeDto;
import com.ercanbeyen.bankingapplication.exception.BadRequestException;
import lombok.experimental.UtilityClass;

import java.util.Set;

@UtilityClass
public class FeeUtil {
    private final Set<Integer> DEPOSIT_PERIODS = Set.of(1, 3, 6, 12);

    public void checkRequest(FeeDto request) {
        if (request.getMaximumAmount() <= request.getMinimumAmount()) {
            throw new BadRequestException("Minimum amount should be less than the maximum amount");
        }

        checkValidityOfDepositPeriod(request.getDepositPeriod());
    }

    public void checkValidityOfDepositPeriod(Integer depositPeriod) {
        if (!DEPOSIT_PERIODS.contains(depositPeriod)) {
            throw new BadRequestException("Deposit period is invalid. Valid values are " + DEPOSIT_PERIODS);
        }
    }
}
