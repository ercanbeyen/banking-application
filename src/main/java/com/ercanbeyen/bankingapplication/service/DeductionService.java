package com.ercanbeyen.bankingapplication.service;

import com.ercanbeyen.bankingapplication.constant.enums.ActivityType;
import com.ercanbeyen.bankingapplication.dto.DeductionDto;

import java.util.List;

public interface DeductionService {
    DeductionDto createDeduction(DeductionDto request);
    DeductionDto updateDeduction(ActivityType activityType, DeductionDto request);
    List<DeductionDto> getDeductions();
    DeductionDto getDeduction(ActivityType activityType);
    void deleteDeduction(ActivityType activityType);
}
