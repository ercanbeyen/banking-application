package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.LogMessage;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.dto.DailyActivityLimitDto;
import com.ercanbeyen.bankingapplication.entity.DailyActivityLimit;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.mapper.DailyActivityLimitMapper;
import com.ercanbeyen.bankingapplication.option.DailyActivityLimitFilteringOption;
import com.ercanbeyen.bankingapplication.repository.DailyActivityRepository;
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
public class DailyActivityLimitService implements BaseService<DailyActivityLimitDto, DailyActivityLimitFilteringOption> {
    private final DailyActivityRepository dailyActivityLimitRepository;
    private final DailyActivityLimitMapper dailyActivityLimitMapper;

    @Override
    public List<DailyActivityLimitDto> getEntities(DailyActivityLimitFilteringOption filteringOption) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Predicate<DailyActivityLimit> dailyActivityLimitPredicate = dailyActivityLimit -> {
            LocalDate createdAt = filteringOption.getCreatedAt();
            LocalDate updatedAt = filteringOption.getUpdatedAt();
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
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        return dailyActivityLimitRepository.findById(id)
                .map(dailyActivityLimitMapper::entityToDto)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, Entity.DAILY_ACTIVITY_LIMIT.getValue())));
    }

    @Override
    public DailyActivityLimitDto createEntity(DailyActivityLimitDto request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        checkUniqueness(request, null);

        DailyActivityLimit dailyActivityLimit = dailyActivityLimitMapper.dtoToEntity(request);
        DailyActivityLimit savedDailyActivityLimit = dailyActivityLimitRepository.save(dailyActivityLimit);
        log.info(LogMessage.RESOURCE_CREATE_SUCCESS, Entity.DAILY_ACTIVITY_LIMIT.getValue(), savedDailyActivityLimit.getId());

        return dailyActivityLimitMapper.entityToDto(savedDailyActivityLimit);
    }

    @Override
    public DailyActivityLimitDto updateEntity(Integer id, DailyActivityLimitDto request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        DailyActivityLimit dailyActivityLimit = findById(id);
        checkUniqueness(request, dailyActivityLimit.getActivityType());

        dailyActivityLimit.setActivityType(request.getActivityType());
        dailyActivityLimit.setAmount(request.getAmount());

        return dailyActivityLimitMapper.entityToDto(dailyActivityLimitRepository.save(dailyActivityLimit));
    }

    @Override
    public void deleteEntity(Integer id) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        String entity = Entity.DAILY_ACTIVITY_LIMIT.getValue();

        dailyActivityLimitRepository.findById(id)
                .ifPresentOrElse(dailyActivityLimit -> {
                    log.info(LogMessage.RESOURCE_FOUND, entity);
                    dailyActivityLimitRepository.deleteById(id);
                }, () -> {
                    log.error(LogMessage.RESOURCE_NOT_FOUND, entity);
                    throw new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, entity));
                });

        log.info(LogMessage.RESOURCE_DELETE_SUCCESS, entity, id);
    }

    public Double getAmountByActivityType(AccountActivityType activityType) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        String entity = Entity.DAILY_ACTIVITY_LIMIT.getValue();
        DailyActivityLimit dailyActivityLimit = dailyActivityLimitRepository.findByActivityType(activityType)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, entity)));

        Double amount = dailyActivityLimit.getAmount();
        log.info("Charge amount of {}: {}", activityType.getValue(), amount);

        return amount;
    }

    private DailyActivityLimit findById(Integer id) {
        String entity = Entity.DAILY_ACTIVITY_LIMIT.getValue();
        DailyActivityLimit dailyActivityLimit = dailyActivityLimitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, entity)));

        log.info(LogMessage.RESOURCE_FOUND, entity);

        return dailyActivityLimit;
    }

    private void checkUniqueness(DailyActivityLimitDto request, AccountActivityType previousActivityType) {
        String entity = Entity.DAILY_ACTIVITY_LIMIT.getValue();

        if (previousActivityType == request.getActivityType()) {
            log.warn(LogMessage.NO_ACCOUNT_ACTIVITY_CHANGE, entity);
            return;
        }

        boolean entityExists = dailyActivityLimitRepository.existsByActivityType(request.getActivityType());

        if (entityExists) {
            throw new ResourceConflictException(String.format(ResponseMessage.ALREADY_EXISTS, entity));
        }

        log.info(LogMessage.RESOURCE_UNIQUE, entity);
    }
}
