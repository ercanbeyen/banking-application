package com.ercanbeyen.bankingapplication.factory;

import com.ercanbeyen.bankingapplication.entity.CashFlowCalendar;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MockCashFlowCalendarFactory {
    private MockCashFlowCalendarFactory() {}

    public static List<CashFlowCalendar> generateMockCashFlowCalendars() {
        CashFlowCalendar cashFlowCalendar1 = new CashFlowCalendar();
        cashFlowCalendar1.setId(UUID.randomUUID().toString());
        cashFlowCalendar1.setCashFlows(new ArrayList<>());

        CashFlowCalendar cashFlowCalendar2 = new CashFlowCalendar();
        cashFlowCalendar2.setId(UUID.randomUUID().toString());
        cashFlowCalendar2.setCashFlows(new ArrayList<>());

        CashFlowCalendar cashFlowCalendar3 = new CashFlowCalendar();
        cashFlowCalendar3.setId(UUID.randomUUID().toString());
        cashFlowCalendar3.setCashFlows(new ArrayList<>());

        return List.of(cashFlowCalendar1, cashFlowCalendar2, cashFlowCalendar3);
    }
}
