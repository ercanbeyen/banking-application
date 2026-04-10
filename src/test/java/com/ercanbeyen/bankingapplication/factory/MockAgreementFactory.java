package com.ercanbeyen.bankingapplication.factory;

import com.ercanbeyen.bankingapplication.constant.enums.AgreementSubject;
import com.ercanbeyen.bankingapplication.entity.Agreement;

import java.util.HashSet;

public class MockAgreementFactory {
    private MockAgreementFactory() {}

    public static Agreement getMockAgreement() {
        Agreement agreement = new Agreement();
        agreement.setId(null);
        agreement.setTitle("Customer Information");
        agreement.setSubject(AgreementSubject.CUSTOMER);
        agreement.setCustomers(new HashSet<>());

        return agreement;
    }
}
