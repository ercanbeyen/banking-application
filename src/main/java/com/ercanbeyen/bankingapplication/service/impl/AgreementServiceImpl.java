package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.AgreementSubject;
import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.LogMessage;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.dto.AgreementDto;
import com.ercanbeyen.bankingapplication.entity.Agreement;
import com.ercanbeyen.bankingapplication.entity.Customer;
import com.ercanbeyen.bankingapplication.entity.CustomerAgreement;
import com.ercanbeyen.bankingapplication.entity.File;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.mapper.AgreementMapper;
import com.ercanbeyen.bankingapplication.repository.AgreementRepository;
import com.ercanbeyen.bankingapplication.repository.CustomerAgreementRepository;
import com.ercanbeyen.bankingapplication.service.AgreementService;
import com.ercanbeyen.bankingapplication.service.FileService;
import com.ercanbeyen.bankingapplication.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgreementServiceImpl implements AgreementService {
    private final AgreementRepository agreementRepository;
    private final CustomerAgreementRepository customerAgreementRepository;
    private final AgreementMapper agreementMapper;
    private final FileService fileService;

    @Override
    public AgreementDto createAgreement(String title, String subject, MultipartFile request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        if (agreementRepository.existsByTitle(title)) {
            throw new ResourceConflictException(String.format(ResponseMessage.ALREADY_EXISTS, Entity.AGREEMENT.getValue()));
        }

        CompletableFuture<File> fileCompletableFuture = fileService.storeFile(request, title);

        Agreement agreement = new Agreement();
        agreement.setFile(fileCompletableFuture.join());
        agreement.setTitle(title);
        agreement.setSubject(AgreementSubject.valueOf(subject));

        Agreement savedAgreement = agreementRepository.save(agreement);
        log.info(LogMessage.RESOURCE_CREATE_SUCCESS, Entity.AGREEMENT.getValue(), savedAgreement.getId());

        return agreementMapper.entityToDto(savedAgreement);
    }

    @Override
    public AgreementDto updateAgreement(String id, String title, String subject, MultipartFile request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Agreement agreement = findById(id);

        if (!agreement.getTitle().equals(title) && agreementRepository.existsByTitle(title)) {
            throw new ResourceConflictException(String.format(ResponseMessage.ALREADY_EXISTS, Entity.AGREEMENT.getValue()));
        }

        CompletableFuture<File> fileCompletableFuture = fileService.storeFile(request, title);

        agreement.setFile(fileCompletableFuture.join());
        agreement.setTitle(title);
        agreement.setSubject(AgreementSubject.valueOf(subject));

        Agreement savedAgreement = agreementRepository.save(agreement);

        return agreementMapper.entityToDto(savedAgreement);
    }

    @Override
    public List<AgreementDto> getAgreements() {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());
        return agreementRepository.findAll()
                .stream()
                .map(agreementMapper::entityToDto)
                .toList();
    }

    @Override
    public AgreementDto getAgreement(String id) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());
        return agreementMapper.entityToDto(findById(id));
    }

    @Override
    public void approveAgreement(String title, Customer customer) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Agreement agreement = agreementRepository.findByTitle(title)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, Entity.AGREEMENT.getValue())));

        if (customerAgreementRepository.existsByAgreementTitleAndCustomerNationalId(title, customer.getNationalId())) {
            log.error("Customer {} has already been added to {} agreement before", customer.getNationalId(), title);
            throw new ResourceConflictException("Customer has already approved the agreement before");
        }

        log.info("Customer has not approved the agreement {} yet", agreement.getTitle());

        CustomerAgreement customerAgreement = new CustomerAgreement();
        customerAgreement.setCustomer(customer);
        customerAgreement.setAgreement(agreement);

        customerAgreementRepository.save(customerAgreement);
    }

    @Override
    public void approveAgreements(AgreementSubject subject, Customer customer) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        List<Agreement> agreements = agreementRepository.findBySubject(subject);

        if (agreements.isEmpty()) {
            log.error("No agreement is found for subject {}", subject.getValue());
            throw new ResourceNotFoundException("No agreement is found");
        }

        for (Agreement agreement : agreements) {
            String title = agreement.getTitle();

            if (customerAgreementRepository.existsByAgreementTitleAndCustomerNationalId(title, customer.getNationalId())) {
                log.warn("Customer {} has already been added to {} agreement before", customer.getNationalId(), title);
                continue;
            }

            CustomerAgreement customerAgreement = new CustomerAgreement();
            customerAgreement.setCustomer(customer);
            customerAgreement.setAgreement(agreement);

            customerAgreementRepository.save(customerAgreement);

            log.info("Customer {} is successfully added to all agreements of subject {}", customer.getNationalId(), agreement.getTitle());
        }
    }

    @Transactional
    @Override
    public String deleteAgreement(String id) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        String entity = Entity.AGREEMENT.getValue();

        Agreement agreement = findById(id);
        customerAgreementRepository.deleteAllByAgreementTitle(agreement.getTitle());
        agreementRepository.delete(agreement);

        log.info(LogMessage.RESOURCE_DELETE_SUCCESS, entity, id);

        return entity + " " + id + " is successfully deleted";
    }

    private Agreement findById(String id) {
        String entity = Entity.AGREEMENT.getValue();
        Agreement agreement = agreementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, entity)));

        log.info(LogMessage.RESOURCE_FOUND, entity);

        return agreement;
    }
}
