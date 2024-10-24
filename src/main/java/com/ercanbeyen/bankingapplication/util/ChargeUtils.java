package com.ercanbeyen.bankingapplication.util;

import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import com.ercanbeyen.bankingapplication.dto.ChargeDto;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import lombok.experimental.UtilityClass;

import java.util.Set;

@UtilityClass
public class ChargeUtils {
    public void checkRequest(ChargeDto request) {
        Set<AccountActivityType> freeAccountActivities = AccountActivityType.getAccountStatusUpdatingActivities();
        freeAccountActivities.addAll(Set.of(AccountActivityType.MONEY_DEPOSIT, AccountActivityType.WITHDRAWAL));

        if (freeAccountActivities.contains(request.getActivityType())) {
            throw new ResourceConflictException("Account activities " + freeAccountActivities + " are free");
        }
    }
}
