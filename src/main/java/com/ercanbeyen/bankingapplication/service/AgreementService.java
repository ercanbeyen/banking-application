package com.ercanbeyen.bankingapplication.service;

import com.ercanbeyen.bankingapplication.dto.AgreementDto;
import com.ercanbeyen.bankingapplication.entity.Customer;
import org.springframework.web.multipart.MultipartFile;

public interface AgreementService {
    AgreementDto createAgreement(String subject, MultipartFile request);
    AgreementDto updateAgreement(String id, String subject, MultipartFile request);
    AgreementDto getAgreement(String id);
    void addCustomerToAgreement(String subject, Customer customer);
    String deleteAgreement(String id);
}
