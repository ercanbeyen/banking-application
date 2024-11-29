package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.LogMessages;
import com.ercanbeyen.bankingapplication.embeddable.CashFlowPK;
import com.ercanbeyen.bankingapplication.entity.AccountActivity;
import com.ercanbeyen.bankingapplication.entity.CashFlow;
import com.ercanbeyen.bankingapplication.entity.Customer;
import com.ercanbeyen.bankingapplication.repository.CashFlowRepository;
import com.ercanbeyen.bankingapplication.service.CashFlowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class CashFlowServiceImpl implements CashFlowService {
    private final CashFlowRepository cashFlowRepository;

    @Override
    public void createCashFlow(Customer customer, AccountActivity accountActivity) {
        CashFlowPK cashFlowPK = new CashFlowPK(accountActivity.getId(), customer.getNationalId());
        CashFlow cashFlow = new CashFlow();

        cashFlow.setCashFlowPK(cashFlowPK);
        cashFlow.setCashFlowCalendar(customer.getCashFlowCalendar());
        cashFlow.setAccountActivityType(accountActivity.getType());
        cashFlow.setAmount(accountActivity.getAmount());
        cashFlow.setDate(accountActivity.getCreatedAt().toLocalDate());

        CashFlow savedCashFlow = cashFlowRepository.save(cashFlow);
        log.info(LogMessages.RESOURCE_CREATE_SUCCESS, Entity.CASH_FLOW.getValue(), savedCashFlow.getCashFlowPK());
    }
}
