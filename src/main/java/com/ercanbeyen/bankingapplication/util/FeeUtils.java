package com.ercanbeyen.bankingapplication.util;

import com.ercanbeyen.bankingapplication.dto.FeeDto;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import com.ercanbeyen.bankingapplication.exception.ResourceExpectationFailedException;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class FeeUtils {
    private final List<Integer> DEPOSIT_PERIODS = List.of(1, 3, 6, 12);

    public void checkRequest(FeeDto request) {
        if (request.getMaximumAmount() <= request.getMinimumAmount()) {
            throw new ResourceConflictException("Minimum amount should be less than the maximum amount");
        }

        checkValidityOfDepositPeriod(request.getDepositPeriod());
    }

    public void checkValidityOfDepositPeriod(Integer depositPeriod) {
        if (!DEPOSIT_PERIODS.contains(depositPeriod)) {
            throw new ResourceExpectationFailedException("Deposit period is invalid. Valid values are " + DEPOSIT_PERIODS);
        }
    }
}
