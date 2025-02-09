package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.LogMessage;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.dto.ContractDto;
import com.ercanbeyen.bankingapplication.entity.Contract;
import com.ercanbeyen.bankingapplication.entity.Customer;
import com.ercanbeyen.bankingapplication.entity.File;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.mapper.ContractMapper;
import com.ercanbeyen.bankingapplication.repository.ContractRepository;
import com.ercanbeyen.bankingapplication.service.ContractService;
import com.ercanbeyen.bankingapplication.service.FileService;
import com.ercanbeyen.bankingapplication.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContractServiceImpl implements ContractService {
    private final ContractRepository contractRepository;
    private final ContractMapper contractMapper;
    private final FileService fileService;

    @Override
    public ContractDto createContract(ContractDto request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Contract contract = contractMapper.dtoToEntity(request);
        File file = fileService.getFile(request.fileId());

        contract.setFile(file);

        Contract savedContract = contractRepository.save(contract);
        log.info(LogMessage.RESOURCE_CREATE_SUCCESS, Entity.CONTRACT.getValue(), savedContract.getId());

        return contractMapper.entityToDto(savedContract);
    }

    @Override
    public ContractDto updateContract(String id, ContractDto request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Contract contract = findById(id);
        File file = fileService.getFile(request.fileId());

        contract.setFile(file);
        contract.setSubject(request.subject());

        Contract savedContract = contractRepository.save(contract);

        return contractMapper.entityToDto(savedContract);
    }

    @Override
    public ContractDto getContract(String id) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());
        Contract contract = findById(id);
        return contractMapper.entityToDto(contract);
    }

    @Override
    public void addCustomerToContract(String subject, Customer customer) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Contract contract = findContractBySubject(subject);

        if (contractRepository.findBySubjectAndCustomerNationalId(subject, customer.getNationalId()).isPresent()) {
            log.warn("Customer {} has already been added to {} before", customer.getNationalId(), subject);
            return;
        }

        log.info("Customer {} has not added to {} before", customer.getNationalId(), subject);
        contract.getCustomers().add(customer);
        contractRepository.save(contract);

        log.info("Customer {} is successfully added to contract {}", customer.getNationalId(), contract.getSubject());
    }

    @Override
    public String deleteContract(String id) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        String entity = Entity.CONTRACT.getValue();

        contractRepository.findById(id)
                .ifPresentOrElse(contract -> {
                    log.info(LogMessage.RESOURCE_FOUND, entity);
                    contractRepository.deleteById(id);
                }, () -> {
                    log.error(LogMessage.RESOURCE_NOT_FOUND, entity);
                    throw new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, entity));
                });

        log.info(LogMessage.RESOURCE_DELETE_SUCCESS, entity, id);

        return entity + " " + id + " is successfully deleted";
    }

    private Contract findById(String id) {
        String entity = Entity.CONTRACT.getValue();
        Contract contract = contractRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, entity)));

        log.info(LogMessage.RESOURCE_FOUND, entity);

        return contract;
    }

    private Contract findContractBySubject(String subject) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        String entity = Entity.CONTRACT.getValue();
        Contract contract = contractRepository.findBySubject(subject)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, entity)));

        log.info(LogMessage.RESOURCE_FOUND, entity);

        return contract;
    }
}
