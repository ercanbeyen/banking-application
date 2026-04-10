package com.ercanbeyen.bankingapplication.service;

import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import com.ercanbeyen.bankingapplication.dto.DeductionDto;

import java.util.List;

public interface DeductionService {
    DeductionDto createDeduction(DeductionDto request);
    DeductionDto updateDeduction(AccountActivityType activityType, DeductionDto request);
    List<DeductionDto> getDeductions();
    DeductionDto getDeduction(AccountActivityType activityType);
    void deleteDeduction(AccountActivityType activityType);
}
