package com.ercanbeyen.bankingapplication.service;

import com.ercanbeyen.bankingapplication.entity.AccountActivity;
import com.ercanbeyen.bankingapplication.entity.Customer;

public interface CashFlowService {
    void createCashFlow(Customer customer, AccountActivity accountActivity);
}
