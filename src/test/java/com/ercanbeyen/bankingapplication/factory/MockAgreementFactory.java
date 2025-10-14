package com.ercanbeyen.bankingapplication.factory;

import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.entity.Agreement;
import com.ercanbeyen.bankingapplication.util.AgreementUtil;

public class MockAgreementFactory {
    private MockAgreementFactory() {}

    public static Agreement getMockAgreement() {
        Agreement agreement = new Agreement();
        agreement.setId(null);
        agreement.setSubject(AgreementUtil.generateSubject(Entity.CUSTOMER));

        return agreement;
    }
}
