package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.LogMessages;
import com.ercanbeyen.bankingapplication.entity.CashFlowCalendar;
import com.ercanbeyen.bankingapplication.repository.CashFlowCalendarRepository;
import com.ercanbeyen.bankingapplication.service.CashFlowCalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Slf4j
@RequiredArgsConstructor
@Service
public class CashFlowCalendarServiceImpl implements CashFlowCalendarService {
    private final CashFlowCalendarRepository cashFlowCalendarRepository;

    @Override
    public CashFlowCalendar createCashFlowCalendar() {
        CashFlowCalendar cashFlowCalendar = new CashFlowCalendar();
        cashFlowCalendar.setCashFlows(new ArrayList<>());

        CashFlowCalendar savedCashFlowCalendar = cashFlowCalendarRepository.save(cashFlowCalendar);
        log.info(LogMessages.RESOURCE_CREATE_SUCCESS, Entity.CASH_FLOW_CALENDAR.getValue(), savedCashFlowCalendar.getId());

        return savedCashFlowCalendar;
    }
}
