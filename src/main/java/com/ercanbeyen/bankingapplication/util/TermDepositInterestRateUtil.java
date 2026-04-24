package com.ercanbeyen.bankingapplication.util;

import com.ercanbeyen.bankingapplication.dto.TermDepositInterestRateDto;
import com.ercanbeyen.bankingapplication.exception.BadRequestException;
import lombok.experimental.UtilityClass;

import java.util.Set;

@UtilityClass
public class TermDepositInterestRateUtil {
    private final Set<Integer> DEPOSIT_MATURITIES = Set.of(1, 3, 6, 12);

    public void checkRequest(TermDepositInterestRateDto request) {
        if (request.getMaximumBalance() <= request.getMinimumBalance()) {
            throw new BadRequestException("Minimum amount should be less than the maximum amount");
        }

        checkValidityOfDepositMaturity(request.getDepositMaturity());
    }

    public void checkValidityOfDepositMaturity(Integer depositMaturity) {
        if (!DEPOSIT_MATURITIES.contains(depositMaturity)) {
            throw new BadRequestException("Deposit maturity is invalid. Valid values are " + DEPOSIT_MATURITIES);
        }
    }
}
