package com.ercanbeyen.bankingapplication.service;

import com.ercanbeyen.bankingapplication.model.AccountActivity;
import com.ercanbeyen.bankingapplication.model.CashFlowCalendar;

public interface CashFlowCalendarService {
    CashFlowCalendar createCashFlowCalendar();
    void createCashFlow(CashFlowCalendar cashFlowCalendar, AccountActivity accountActivity, String explanation);
}
