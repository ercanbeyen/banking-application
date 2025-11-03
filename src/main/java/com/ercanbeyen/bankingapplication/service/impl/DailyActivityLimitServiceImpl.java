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
import com.ercanbeyen.bankingapplication.repository.DailyActivityLimitRepository;
import com.ercanbeyen.bankingapplication.service.DailyActivityLimitService;
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
public class DailyActivityLimitServiceImpl implements DailyActivityLimitService {
    private final DailyActivityLimitRepository dailyActivityLimitRepository;
    private final DailyActivityLimitMapper dailyActivityLimitMapper;

    @CacheEvict(value = "dailyActivityLimits", allEntries = true)
    @Override
    public List<DailyActivityLimitDto> getDailyActivityLimits() {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        return dailyActivityLimitRepository.findAll()
                .stream()
                .map(dailyActivityLimitMapper::entityToDto)
                .toList();
    }

    @Cacheable(value = "dailyActivityLimits", key = "#a0")
    @Override
    public DailyActivityLimitDto getDailyActivityLimit(AccountActivityType activityType) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());
        DailyActivityLimit dailyActivityLimit = findByActivityType(activityType);
        return dailyActivityLimitMapper.entityToDto(dailyActivityLimit);
    }

    @Override
    public DailyActivityLimitDto createDailyActivityLimit(DailyActivityLimitDto request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        checkUniqueness(request, null);

        DailyActivityLimit dailyActivityLimit = dailyActivityLimitMapper.dtoToEntity(request);
        DailyActivityLimit savedDailyActivityLimit = dailyActivityLimitRepository.save(dailyActivityLimit);
        log.info(LogMessage.RESOURCE_CREATE_SUCCESS, Entity.DAILY_ACTIVITY_LIMIT.getValue(), savedDailyActivityLimit.getId());

        return dailyActivityLimitMapper.entityToDto(savedDailyActivityLimit);
    }

    @CachePut(value = "dailyActivityLimits", key = "#a0")
    @Override
    public DailyActivityLimitDto updateDailyActivityLimit(AccountActivityType activityType, DailyActivityLimitDto request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        DailyActivityLimit dailyActivityLimit = findByActivityType(activityType);
        checkUniqueness(request, dailyActivityLimit.getActivityType());

        dailyActivityLimit.setAmount(request.amount());

        return dailyActivityLimitMapper.entityToDto(dailyActivityLimitRepository.save(dailyActivityLimit));
    }

    @CacheEvict(value = "dailyActivityLimits", key = "#a0")
    @Transactional
    @Override
    public void deleteDailyActivityLimit(AccountActivityType activityType) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        String entity = Entity.DAILY_ACTIVITY_LIMIT.getValue();

        if (!dailyActivityLimitExistsByActivityType(activityType)) {
            throw new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, entity));
        }

        log.info(LogMessage.RESOURCE_FOUND, entity);

        dailyActivityLimitRepository.deleteByActivityType(activityType);

        log.info(LogMessage.RESOURCE_DELETE_SUCCESS, entity, activityType);
    }


    private DailyActivityLimit findByActivityType(AccountActivityType activityType) {
        String entity = Entity.DAILY_ACTIVITY_LIMIT.getValue();
        DailyActivityLimit dailyActivityLimit = dailyActivityLimitRepository.findByActivityType(activityType)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, entity)));

        log.info(LogMessage.RESOURCE_FOUND, entity);

        return dailyActivityLimit;
    }

    private void checkUniqueness(DailyActivityLimitDto request, AccountActivityType previousActivityType) {
        String entity = Entity.DAILY_ACTIVITY_LIMIT.getValue();

        if (previousActivityType == request.activityType()) {
            log.warn(LogMessage.NO_ACCOUNT_ACTIVITY_CHANGE, entity);
            return;
        }

        boolean entityExists = dailyActivityLimitExistsByActivityType(request.activityType());

        if (entityExists) {
            throw new ResourceConflictException(String.format(ResponseMessage.ALREADY_EXISTS, entity));
        }

        log.info(LogMessage.RESOURCE_UNIQUE, entity);
    }

    private boolean dailyActivityLimitExistsByActivityType(AccountActivityType activityType) {
        return dailyActivityLimitRepository.existsByActivityType(activityType);
    }
}
