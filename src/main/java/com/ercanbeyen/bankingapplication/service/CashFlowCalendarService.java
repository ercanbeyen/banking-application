package com.ercanbeyen.bankingapplication.service;

import com.ercanbeyen.bankingapplication.entity.AccountActivity;
import com.ercanbeyen.bankingapplication.entity.CashFlowCalendar;

public interface CashFlowCalendarService {
    CashFlowCalendar createCashFlowCalendar();
    void createCashFlow(CashFlowCalendar cashFlowCalendar, AccountActivity accountActivity);
}
