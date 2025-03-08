package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.LogMessage;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.dto.ChargeDto;
import com.ercanbeyen.bankingapplication.entity.Charge;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.mapper.ChargeMapper;
import com.ercanbeyen.bankingapplication.repository.ChargeRepository;
import com.ercanbeyen.bankingapplication.service.ChargeService;
import com.ercanbeyen.bankingapplication.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class ChargeServiceImpl implements ChargeService {
    private final ChargeRepository chargeRepository;
    private final ChargeMapper chargeMapper;

    public List<ChargeDto> getCharges() {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        return chargeRepository.findAll()
                .stream()
                .map(chargeMapper::entityToDto)
                .toList();
    }

    public ChargeDto createCharge(ChargeDto request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        checkUniqueness(request, null);
        Charge charge = chargeMapper.dtoToEntity(request);

        Charge savedCharge = chargeRepository.save(charge);
        log.info(LogMessage.RESOURCE_CREATE_SUCCESS, Entity.CHARGE.getValue(), savedCharge.getId());

        return chargeMapper.entityToDto(savedCharge);
    }

    @CacheEvict(value = "charges", allEntries = true)
    public ChargeDto updateCharge(AccountActivityType activityType, ChargeDto request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Charge charge = findByActivityType(activityType);
        checkUniqueness(request, charge.getActivityType());

        charge.setActivityType(request.getActivityType());
        charge.setAmount(request.getAmount());

        return chargeMapper.entityToDto(chargeRepository.save(charge));
    }

    @Cacheable(value = "charges", key = "#a0")
    public ChargeDto getCharge(AccountActivityType activityType) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());
        Charge charge = findByActivityType(activityType);

        log.info("We are in getCharge --> No caching");

        return chargeMapper.entityToDto(charge);
    }

    @CacheEvict(value = "charges", key = "#a0")
    @Transactional
    public void deleteCharge(AccountActivityType activityType) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        String entity = Entity.CHARGE.getValue();

        if (!chargeExistsByActivityType(activityType)) {
            throw new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, entity));
        }

        log.info(LogMessage.RESOURCE_FOUND, entity);

        chargeRepository.deleteByActivityType(activityType);

        log.info(LogMessage.RESOURCE_DELETE_SUCCESS, entity, activityType);
    }

    private Charge findByActivityType(AccountActivityType activityType) {
        String entity = Entity.CHARGE.getValue();
        Charge charge = chargeRepository.findByActivityType(activityType)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, entity)));

        log.info(LogMessage.RESOURCE_FOUND, entity);
        return charge;
    }

    private void checkUniqueness(ChargeDto request, AccountActivityType previousActivityType) {
        String entity = Entity.CHARGE.getValue();

        if (previousActivityType == request.getActivityType()) {
            log.warn(LogMessage.NO_ACCOUNT_ACTIVITY_CHANGE, entity);
            return;
        }

        boolean entityExists = chargeExistsByActivityType(request.getActivityType());

        if (entityExists) {
            throw new ResourceConflictException(String.format(ResponseMessage.ALREADY_EXISTS, entity));
        }

        log.info(LogMessage.RESOURCE_UNIQUE, entity);
    }

    private boolean chargeExistsByActivityType(AccountActivityType activityType) {
        return chargeRepository.existsByActivityType(activityType);
    }
}
