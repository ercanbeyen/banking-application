package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.LogMessage;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.dto.DeductionDto;
import com.ercanbeyen.bankingapplication.entity.Deduction;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.mapper.DeductionMapper;
import com.ercanbeyen.bankingapplication.repository.DeductionRepository;
import com.ercanbeyen.bankingapplication.service.DeductionService;
import com.ercanbeyen.bankingapplication.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class DeductionServiceImpl implements DeductionService {
    private final DeductionRepository deductionRepository;
    private final DeductionMapper deductionMapper;

    @CacheEvict(value = "deductions", allEntries = true)
    @Override
    public List<DeductionDto> getDeductions() {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        return deductionRepository.findAll()
                .stream()
                .map(deductionMapper::entityToDto)
                .toList();
    }

    @Override
    public DeductionDto createDeduction(DeductionDto request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        checkUniqueness(request, null);
        Deduction deduction = deductionMapper.dtoToEntity(request);

        Deduction savedDeduction = deductionRepository.save(deduction);
        log.info(LogMessage.RESOURCE_CREATE_SUCCESS, Entity.DEDUCTION.getValue(), savedDeduction.getId());

        return deductionMapper.entityToDto(savedDeduction);
    }

    @CachePut(value = "deductions", key = "#a0")
    @Override
    public DeductionDto updateDeduction(AccountActivityType accountActivityType, DeductionDto request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Deduction deduction = findByActivityType(accountActivityType);
        checkUniqueness(request, deduction.getAccountActivityType());

        deduction.setAmount(request.amount());

        return deductionMapper.entityToDto(deductionRepository.save(deduction));
    }

    @Cacheable(value = "deductions", key = "#a0")
    @Override
    public DeductionDto getDeduction(AccountActivityType accountActivityType) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());
        Deduction deduction = findByActivityType(accountActivityType);
        return deductionMapper.entityToDto(deduction);
    }

    @CacheEvict(value = "deductions", key = "#a0")
    @Transactional
    @Override
    public void deleteDeduction(AccountActivityType accountActivityType) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        String entity = Entity.DEDUCTION.getValue();

        if (!deductionExistsByAccountActivityType(accountActivityType)) {
            throw new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, entity));
        }

        log.info(LogMessage.RESOURCE_FOUND, entity);

        deductionRepository.deleteByAccountActivityType(accountActivityType);

        log.info(LogMessage.RESOURCE_DELETE_SUCCESS, entity, accountActivityType);
    }

    private Deduction findByActivityType(AccountActivityType accountActivityType) {
        String entity = Entity.DEDUCTION.getValue();
        Deduction deduction = deductionRepository.findByAccountActivityType(accountActivityType)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, entity)));

        log.info(LogMessage.RESOURCE_FOUND, entity);

        return deduction;
    }

    private void checkUniqueness(DeductionDto request, AccountActivityType previousAccountActivityType) {
        String entity = Entity.DEDUCTION.getValue();

        if (previousAccountActivityType == request.accountActivityType()) {
            log.warn(LogMessage.NO_ACCOUNT_ACTIVITY_CHANGE, entity);
            return;
        }

        boolean entityExists = deductionExistsByAccountActivityType(request.accountActivityType());

        if (entityExists) {
            throw new ResourceConflictException(String.format(ResponseMessage.ALREADY_EXISTS, entity));
        }

        log.info(LogMessage.RESOURCE_UNIQUE, entity);
    }

    private boolean deductionExistsByAccountActivityType(AccountActivityType activityType) {
        return deductionRepository.existsByAccountActivityType(activityType);
    }
}
