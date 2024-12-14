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
import com.ercanbeyen.bankingapplication.option.ChargeFilteringOption;
import com.ercanbeyen.bankingapplication.repository.ChargeRepository;
import com.ercanbeyen.bankingapplication.service.BaseService;
import com.ercanbeyen.bankingapplication.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Slf4j
@RequiredArgsConstructor
@Service
public class ChargeService implements BaseService<ChargeDto, ChargeFilteringOption> {
    private final ChargeRepository chargeRepository;
    private final ChargeMapper chargeMapper;

    @Override
    public List<ChargeDto> getEntities(ChargeFilteringOption filteringOption) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Predicate<Charge> chargePredicate = charge -> {
            LocalDate createdAt = filteringOption.getCreatedAt();
            LocalDate updatedAt = filteringOption.getUpdatedAt();
            boolean createdAtFilter = (Optional.ofNullable(createdAt).isEmpty() || createdAt.isEqual(charge.getCreatedAt().toLocalDate()));
            boolean updatedAtFilter = (Optional.ofNullable(updatedAt).isEmpty() || updatedAt.isEqual(charge.getUpdatedAt().toLocalDate()));
            return createdAtFilter && updatedAtFilter;
        };

        return chargeRepository.findAll()
                .stream()
                .filter(chargePredicate)
                .map(chargeMapper::entityToDto)
                .toList();
    }

    @Override
    public ChargeDto getEntity(Integer id) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());
        return chargeMapper.entityToDto(findById(id));
    }

    @Override
    public ChargeDto createEntity(ChargeDto request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        checkUniqueness(request, null);
        Charge charge = chargeMapper.dtoToEntity(request);

        Charge savedCharge = chargeRepository.save(charge);
        log.info(LogMessage.RESOURCE_CREATE_SUCCESS, Entity.CHARGE.getValue(), savedCharge.getId());

        return chargeMapper.entityToDto(savedCharge);
    }

    @Override
    public ChargeDto updateEntity(Integer id, ChargeDto request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Charge charge = findById(id);
        checkUniqueness(request, charge.getActivityType());

        charge.setActivityType(request.getActivityType());
        charge.setAmount(request.getAmount());

        return chargeMapper.entityToDto(chargeRepository.save(charge));
    }

    @Override
    public void deleteEntity(Integer id) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        String entity = Entity.CHARGE.getValue();

        chargeRepository.findById(id)
                .ifPresentOrElse(charge -> {
                    log.info(LogMessage.RESOURCE_FOUND, entity);
                    chargeRepository.deleteById(id);
                }, () -> {
                    log.error(LogMessage.RESOURCE_NOT_FOUND, entity);
                    throw new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, entity));
                });

        log.info(LogMessage.RESOURCE_DELETE_SUCCESS, entity, id);
    }

    public Double getAmountByActivityType(AccountActivityType activityType) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        String entity = Entity.CHARGE.getValue();
        Charge charge = chargeRepository.findByActivityType(activityType)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, entity)));

        log.info("Charge amount of {}: {}", activityType.getValue(), charge.getAmount());

        return charge.getAmount();
    }

    private Charge findById(Integer id) {
        String entity = Entity.CHARGE.getValue();
        Charge charge = chargeRepository.findById(id)
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

        boolean entityExists = chargeRepository.existsByActivityType(request.getActivityType());

        if (entityExists) {
            throw new ResourceConflictException(String.format(ResponseMessage.ALREADY_EXISTS, entity));
        }

        log.info(LogMessage.RESOURCE_UNIQUE, entity);
    }
}
