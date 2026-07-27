package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.AgreementSubject;
import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.LogMessage;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.dto.AgreementDto;
import com.ercanbeyen.bankingapplication.dto.request.FileUploadRequest;
import com.ercanbeyen.bankingapplication.entity.Agreement;
import com.ercanbeyen.bankingapplication.entity.Customer;
import com.ercanbeyen.bankingapplication.entity.CustomerAgreement;
import com.ercanbeyen.bankingapplication.entity.File;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import com.ercanbeyen.bankingapplication.exception.ResourceExpectationFailedException;
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

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgreementServiceImpl implements AgreementService {
    private final AgreementRepository agreementRepository;
    private final CustomerAgreementRepository customerAgreementRepository;
    private final AgreementMapper agreementMapper;
    private final FileService fileService;

    @Override
    public void createAgreement(String title, String subject, List<FileUploadRequest> fileUploadRequests) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

            if (agreementRepository.existsByTitle(title)) {
                throw new ResourceConflictException(String.format(ResponseMessage.ALREADY_EXISTS, Entity.AGREEMENT.getValue()));
            }

            fileService.saveFiles(fileUploadRequests).thenAccept(files -> {
                List<String> fileNames = files.stream()
                        .map(File::getName)
                        .toList();

                log.info(LogMessage.POSITIVE_CALLBACK_MESSAGE, fileNames);

                Agreement agreement = new Agreement();
                agreement.setTitle(title);
                agreement.setFiles(Set.copyOf(files));
                agreement.setSubject(AgreementSubject.valueOf(subject));

                agreementRepository.save(agreement);
            }).exceptionally(exception -> {
                log.error(LogMessage.NEGATIVE_CALLBACK_MESSAGE, exception.getMessage());
                throw new ResourceExpectationFailedException(exception.getMessage());
            });
    }

    @Override
    public void updateAgreement(String id, String title, String subject, List<FileUploadRequest> fileUploadRequests) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        agreementRepository.findById(id).ifPresentOrElse(agreement -> {
            if (!agreement.getTitle().equals(title) && agreementRepository.existsByTitle(title)) {
                throw new ResourceConflictException(String.format(ResponseMessage.ALREADY_EXISTS, Entity.AGREEMENT.getValue()));
            }

            fileService.saveFiles(fileUploadRequests).thenAccept(files -> {
                List<String> fileNames = files.stream()
                        .map(File::getName)
                        .toList();

                log.info(LogMessage.POSITIVE_CALLBACK_MESSAGE, fileNames);

                agreement.setTitle(title);
                agreement.setFiles(Set.copyOf(files));
                agreement.setSubject(AgreementSubject.valueOf(subject));

                agreementRepository.save(agreement);
            }).exceptionally(exception -> {
                log.error(LogMessage.NEGATIVE_CALLBACK_MESSAGE, exception.getMessage());
                throw new ResourceExpectationFailedException(exception.getMessage());
            });
        }, () -> {
            log.error(LogMessage.RESOURCE_NOT_FOUND, Entity.AGREEMENT.getValue());
            throw new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, Entity.AGREEMENT.getValue()));
        });
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
                log.warn("{} {} has already been added to {} agreement before", Entity.CUSTOMER.getValue(), customer.getNationalId(), title);
                continue;
            }

            CustomerAgreement customerAgreement = new CustomerAgreement();
            customerAgreement.setCustomer(customer);
            customerAgreement.setAgreement(agreement);

            customerAgreementRepository.save(customerAgreement);

            log.info("{} {} is successfully added to all agreements of subject {}", Entity.CUSTOMER.getValue(), customer.getNationalId(), agreement.getTitle());
        }
    }

    @Transactional
    @Override
    public void deleteAgreement(String id) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Agreement agreement = findById(id);
        customerAgreementRepository.deleteAllByAgreementTitle(agreement.getTitle());
        agreementRepository.delete(agreement);

        log.info(LogMessage.RESOURCE_DELETE_SUCCESS, Entity.AGREEMENT.getValue(), id);
    }

    private Agreement findById(String id) {
        String entity = Entity.AGREEMENT.getValue();
        Agreement agreement = agreementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, entity)));

        log.info(LogMessage.RESOURCE_FOUND, entity);

        return agreement;
    }
}
