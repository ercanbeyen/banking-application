package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.LogMessage;
import com.ercanbeyen.bankingapplication.embeddable.CashFlow;
import com.ercanbeyen.bankingapplication.entity.AccountActivity;
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
        log.info(LogMessage.RESOURCE_CREATE_SUCCESS, Entity.CASH_FLOW_CALENDAR.getValue(), savedCashFlowCalendar.getId());

        return savedCashFlowCalendar;
    }

    public void createCashFlow(CashFlowCalendar cashFlowCalendar, AccountActivity accountActivity, String explanation) {
        CashFlow cashFlow = new CashFlow();

        cashFlow.setDate(accountActivity.getCreatedAt().toLocalDate());
        cashFlow.setExplanation(explanation);

        cashFlowCalendar.getCashFlows().add(cashFlow);

        cashFlowCalendarRepository.save(cashFlowCalendar);
    }
}
