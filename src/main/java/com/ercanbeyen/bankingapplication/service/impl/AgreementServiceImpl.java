package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.AgreementSubject;
import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.LogMessage;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.dto.AgreementDto;
import com.ercanbeyen.bankingapplication.entity.Agreement;
import com.ercanbeyen.bankingapplication.entity.Customer;
import com.ercanbeyen.bankingapplication.entity.File;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.mapper.AgreementMapper;
import com.ercanbeyen.bankingapplication.repository.AgreementRepository;
import com.ercanbeyen.bankingapplication.service.AgreementService;
import com.ercanbeyen.bankingapplication.service.FileService;
import com.ercanbeyen.bankingapplication.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgreementServiceImpl implements AgreementService {
    private final AgreementRepository agreementRepository;
    private final AgreementMapper agreementMapper;
    private final FileService fileService;

    @Override
    public AgreementDto createAgreement(String title, String subject, MultipartFile request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

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
        Agreement agreement = findById(id);
        return agreementMapper.entityToDto(agreement);
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

            if (agreementRepository.findByTitleAndCustomerNationalId(title, customer.getNationalId()).isPresent()) {
                log.warn("Customer {} has already been added to {} agreement before", customer.getNationalId(), title);
                continue;
            }

            agreement.getCustomers().add(customer);
            agreementRepository.save(agreement);
            log.info("Customer {} is successfully added to all agreements of subject {}", customer.getNationalId(), agreement.getTitle());
        }
    }

    @Override
    public String deleteAgreement(String id) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        String entity = Entity.AGREEMENT.getValue();

        agreementRepository.findById(id)
                .ifPresentOrElse(_ -> {
                    log.info(LogMessage.RESOURCE_FOUND, entity);
                    agreementRepository.deleteById(id);
                }, () -> {
                    log.error(LogMessage.RESOURCE_NOT_FOUND, entity);
                    throw new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, entity));
                });

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
