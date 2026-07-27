package com.ercanbeyen.bankingapplication.service;

import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import com.ercanbeyen.bankingapplication.dto.DailyAccountActivityLimitDto;

import java.util.List;

public interface DailyAccountActivityLimitService {
    List<DailyAccountActivityLimitDto> getDailyAccountActivityLimits();
    DailyAccountActivityLimitDto getDailyAccountActivityLimit(AccountActivityType activityType);
    DailyAccountActivityLimitDto createDailyAccountActivityLimit(DailyAccountActivityLimitDto request);
    DailyAccountActivityLimitDto updateDailyAccountActivityLimit(AccountActivityType activityType, DailyAccountActivityLimitDto request);
    void deleteDailyAccountActivityLimit(AccountActivityType activityType);
}
