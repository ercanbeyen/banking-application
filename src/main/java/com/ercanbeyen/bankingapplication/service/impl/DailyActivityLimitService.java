package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.LogMessages;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessages;
import com.ercanbeyen.bankingapplication.dto.DailyActivityLimitDto;
import com.ercanbeyen.bankingapplication.entity.DailyActivityLimit;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.mapper.DailyActivityLimitMapper;
import com.ercanbeyen.bankingapplication.option.DailyActivityLimitFilteringOptions;
import com.ercanbeyen.bankingapplication.repository.DailyActivityRepository;
import com.ercanbeyen.bankingapplication.service.BaseService;
import com.ercanbeyen.bankingapplication.util.LoggingUtils;
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
public class DailyActivityLimitService implements BaseService<DailyActivityLimitDto, DailyActivityLimitFilteringOptions> {
    private final DailyActivityRepository dailyActivityLimitRepository;
    private final DailyActivityLimitMapper dailyActivityLimitMapper;

    @Override
    public List<DailyActivityLimitDto> getEntities(DailyActivityLimitFilteringOptions options) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        Predicate<DailyActivityLimit> dailyActivityLimitPredicate = dailyActivityLimit -> {
            LocalDate createdAt = options.getCreatedAt();
            LocalDate updatedAt = options.getUpdatedAt();
            boolean createdAtFilter = (Optional.ofNullable(createdAt).isEmpty() || createdAt.isEqual(dailyActivityLimit.getCreatedAt().toLocalDate()));
            boolean updatedAtFilter = (Optional.ofNullable(updatedAt).isEmpty() || updatedAt.isEqual(dailyActivityLimit.getUpdatedAt().toLocalDate()));
            return createdAtFilter && updatedAtFilter;
        };

        return dailyActivityLimitRepository.findAll()
                .stream()
                .filter(dailyActivityLimitPredicate)
                .map(dailyActivityLimitMapper::entityToDto)
                .toList();
    }

    @Override
    public DailyActivityLimitDto getEntity(Integer id) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        return dailyActivityLimitRepository.findById(id)
                .map(dailyActivityLimitMapper::entityToDto)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessages.NOT_FOUND, Entity.DAILY_ACTIVITY_LIMIT.getValue())));
    }

    @Override
    public DailyActivityLimitDto createEntity(DailyActivityLimitDto request) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        checkUniqueness(request, null);

        DailyActivityLimit dailyActivityLimit = dailyActivityLimitMapper.dtoToEntity(request);
        DailyActivityLimit savedDailyActivityLimit = dailyActivityLimitRepository.save(dailyActivityLimit);
        log.info(LogMessages.RESOURCE_CREATE_SUCCESS, Entity.DAILY_ACTIVITY_LIMIT.getValue(), savedDailyActivityLimit.getId());

        return dailyActivityLimitMapper.entityToDto(savedDailyActivityLimit);
    }

    @Override
    public DailyActivityLimitDto updateEntity(Integer id, DailyActivityLimitDto request) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        DailyActivityLimit dailyActivityLimit = findById(id);
        checkUniqueness(request, dailyActivityLimit.getActivityType());

        dailyActivityLimit.setActivityType(request.getActivityType());
        dailyActivityLimit.setAmount(request.getAmount());

        return dailyActivityLimitMapper.entityToDto(dailyActivityLimitRepository.save(dailyActivityLimit));
    }

    @Override
    public void deleteEntity(Integer id) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        String entity = Entity.DAILY_ACTIVITY_LIMIT.getValue();

        dailyActivityLimitRepository.findById(id)
                .ifPresentOrElse(dailyActivityLimit -> dailyActivityLimitRepository.deleteById(id), () -> {
                    log.error(LogMessages.RESOURCE_NOT_FOUND, entity);
                    throw new ResourceNotFoundException(String.format(ResponseMessages.NOT_FOUND, entity));
                });

        log.info(LogMessages.RESOURCE_DELETE_SUCCESS, entity, id);
    }

    public Double getAmountByActivityType(AccountActivityType activityType) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        String entity = Entity.DAILY_ACTIVITY_LIMIT.getValue();
        DailyActivityLimit dailyActivityLimit = dailyActivityLimitRepository.findByActivityType(activityType)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessages.NOT_FOUND, entity)));

        Double amount = dailyActivityLimit.getAmount();
        log.info("Charge amount of {}: {}", activityType.getValue(), amount);

        return amount;
    }

    private DailyActivityLimit findById(Integer id) {
        String entity =  Entity.DAILY_ACTIVITY_LIMIT.getValue();
        DailyActivityLimit dailyActivityLimit = dailyActivityLimitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessages.NOT_FOUND, entity)));

        log.info(LogMessages.RESOURCE_FOUND, entity);

        return dailyActivityLimit;
    }

    private void checkUniqueness(DailyActivityLimitDto request, AccountActivityType previousActivityType) {
        String entity = Entity.DAILY_ACTIVITY_LIMIT.getValue();

        if (previousActivityType == request.getActivityType()) {
            log.warn(LogMessages.NO_ACCOUNT_ACTIVITY_CHANGE, entity);
            return;
        }

        boolean entityExists = dailyActivityLimitRepository.existsByActivityType(request.getActivityType());

        if (entityExists) {
            throw new ResourceConflictException(String.format(ResponseMessages.ALREADY_EXISTS, entity));
        }

        log.info(LogMessages.RESOURCE_UNIQUE, entity);
    }
}
