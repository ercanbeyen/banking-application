package com.ercanbeyen.bankingapplication.factory;

import com.ercanbeyen.bankingapplication.entity.Contract;

import java.util.UUID;

public class MockContractFactory {
    private MockContractFactory() {}

    public static Contract getMockContract() {
        Contract contract = new Contract();
        contract.setId(UUID.randomUUID().toString());
        contract.setSubject("CUSTOMER_REGISTRATION_CONTRACT");

        return contract;
    }
}
