package com.ercanbeyen.bankingapplication.service;

import com.ercanbeyen.bankingapplication.constant.enums.ActivityType;
import com.ercanbeyen.bankingapplication.dto.DailyActivityLimitDto;

import java.util.List;

public interface DailyActivityLimitService {
    List<DailyActivityLimitDto> getDailyActivityLimits();
    DailyActivityLimitDto getDailyActivityLimit(ActivityType activityType);
    DailyActivityLimitDto createDailyActivityLimit(DailyActivityLimitDto request);
    DailyActivityLimitDto updateDailyActivityLimit(ActivityType activityType, DailyActivityLimitDto request);
    void deleteDailyActivityLimit(ActivityType activityType);
}
