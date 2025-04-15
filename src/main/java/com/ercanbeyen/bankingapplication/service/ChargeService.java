package com.ercanbeyen.bankingapplication.service;

import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import com.ercanbeyen.bankingapplication.dto.ChargeDto;

import java.util.List;

public interface ChargeService {
    ChargeDto createCharge(ChargeDto request);
    ChargeDto updateCharge(AccountActivityType activityType, ChargeDto request);
    List<ChargeDto> getCharges();
    ChargeDto getCharge(AccountActivityType activityType);
    void deleteCharge(AccountActivityType activityType);
}
