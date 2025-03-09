package com.ercanbeyen.bankingapplication.service;

import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import com.ercanbeyen.bankingapplication.dto.DailyActivityLimitDto;

import java.util.List;

public interface DailyActivityLimitService {
    List<DailyActivityLimitDto> getDailyActivityLimits();
    DailyActivityLimitDto getDailyActivityLimit(AccountActivityType activityType);
    DailyActivityLimitDto createDailyActivityLimit(DailyActivityLimitDto request);
    DailyActivityLimitDto updateDailyActivityLimit(AccountActivityType activityType, DailyActivityLimitDto request);
    void deleteDailyActivityLimit(AccountActivityType activityType);
}
