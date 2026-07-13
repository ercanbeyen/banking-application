package com.ercanbeyen.bankingapplication.service;

import com.ercanbeyen.bankingapplication.constant.enums.AgreementSubject;
import com.ercanbeyen.bankingapplication.dto.AgreementDto;
import com.ercanbeyen.bankingapplication.dto.request.FileUploadRequest;
import com.ercanbeyen.bankingapplication.entity.Customer;

import java.util.List;

public interface AgreementService {
    void createAgreement(String title, String subject, List<FileUploadRequest> fileUploadRequests);
    void updateAgreement(String id, String title, String subject, List<FileUploadRequest> fileUploadRequests);
    List<AgreementDto> getAgreements();
    AgreementDto getAgreement(String id);
    void approveAgreement(String title, Customer customer);
    void approveAgreements(AgreementSubject subject, Customer customer);
    void deleteAgreement(String id);
}
