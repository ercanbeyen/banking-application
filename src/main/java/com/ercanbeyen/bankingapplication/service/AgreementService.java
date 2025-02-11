package com.ercanbeyen.bankingapplication.service;

import com.ercanbeyen.bankingapplication.dto.AgreementDto;
import com.ercanbeyen.bankingapplication.entity.Customer;

public interface AgreementService {
    AgreementDto createAgreement(AgreementDto request);
    AgreementDto updateAgreement(String id, AgreementDto request);
    AgreementDto getAgreement(String id);
    void addCustomerToAgreement(String subject, Customer customer);
    String deleteAgreement(String id);
}
