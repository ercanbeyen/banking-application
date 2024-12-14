package com.ercanbeyen.bankingapplication.dto;

import com.ercanbeyen.bankingapplication.embeddable.CashFlow;

import java.util.List;

public record CashFlowCalendarDto(String id, String customerNationalId, List<CashFlow> cashFlows) {

}
