package com.ercanbeyen.bankingapplication.util;

import com.ercanbeyen.bankingapplication.constant.enums.ActivityType;
import com.ercanbeyen.bankingapplication.dto.BranchOrderDto;
import com.ercanbeyen.bankingapplication.exception.BadRequestException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@UtilityClass
@Slf4j
public class BranchOrderUtil {
    private final List<ActivityType> validActivityTypes = List.of(ActivityType.MONEY_DEPOSIT, ActivityType.WITHDRAWAL, ActivityType.MONEY_TRANSFER, ActivityType.MONEY_EXCHANGE);

    public void checkRequest(BranchOrderDto request) {
        if (!validActivityTypes.contains(request.activityType())) {
            throw new BadRequestException("Invalid activity type!");
        }
    }
}
