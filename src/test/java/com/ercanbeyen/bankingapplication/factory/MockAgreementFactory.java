package com.ercanbeyen.bankingapplication.factory;

import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.entity.Agreement;
import com.ercanbeyen.bankingapplication.util.AgreementUtil;

import java.util.UUID;

public class MockAgreementFactory {
    private MockAgreementFactory() {}

    public static Agreement getMockContract() {
        Agreement agreement = new Agreement();
        agreement.setId(UUID.randomUUID().toString());
        agreement.setSubject(AgreementUtil.generateSubject(Entity.CUSTOMER));

        return agreement;
    }
}
