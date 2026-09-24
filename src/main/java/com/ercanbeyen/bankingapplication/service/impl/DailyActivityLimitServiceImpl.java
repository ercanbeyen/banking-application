package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.ActivityType;
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

    @CacheEvict(value = "daily-activity-limits", allEntries = true)
    @Override
    public List<DailyActivityLimitDto> getDailyActivityLimits() {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());
        return dailyActivityLimitRepository.findAll()
                .stream()
                .map(dailyActivityLimitMapper::entityToDto)
                .toList();
    }

    @Cacheable(value = "daily-activity-limits", key = "#a0")
    @Override
    public DailyActivityLimitDto getDailyActivityLimit(ActivityType activityType) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());
        return dailyActivityLimitMapper.entityToDto(findByActivityType(activityType));
    }

    @Override
    public DailyActivityLimitDto createDailyActivityLimit(DailyActivityLimitDto request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        checkUniqueness(request, null);

        DailyActivityLimit dailyActivityLimit = dailyActivityLimitRepository.save(dailyActivityLimitMapper.dtoToEntity(request));
        log.info(LogMessage.RESOURCE_CREATE_SUCCESS, Entity.DAILY_ACTIVITY_LIMIT.getValue(), dailyActivityLimit.getId());

        return dailyActivityLimitMapper.entityToDto(dailyActivityLimit);
    }

    @CachePut(value = "daily-activity-limits", key = "#a0")
    @Override
    public DailyActivityLimitDto updateDailyActivityLimit(ActivityType activityType, DailyActivityLimitDto request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        DailyActivityLimit dailyActivityLimit = findByActivityType(activityType);
        checkUniqueness(request, dailyActivityLimit.getActivityType());

        dailyActivityLimit.setLowerLimit(request.lowerLimit());
        dailyActivityLimit.setUpperLimit(request.upperLimit());

        return dailyActivityLimitMapper.entityToDto(dailyActivityLimitRepository.save(dailyActivityLimit));
    }

    @CacheEvict(value = "daily-activity-limits", key = "#a0")
    @Transactional
    @Override
    public void deleteDailyActivityLimit(ActivityType activityType) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        String entity = Entity.DAILY_ACTIVITY_LIMIT.getValue();

        if (!dailyActivityLimitExistsByActivityType(activityType)) {
            throw new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, entity));
        }

        log.info(LogMessage.RESOURCE_FOUND, entity);

        dailyActivityLimitRepository.deleteByActivityType(activityType);

        log.info(LogMessage.RESOURCE_DELETE_SUCCESS, entity, activityType);
    }


    private DailyActivityLimit findByActivityType(ActivityType activityType) {
        String entity = Entity.DAILY_ACTIVITY_LIMIT.getValue();
        DailyActivityLimit dailyActivityLimit = dailyActivityLimitRepository.findByActivityType(activityType)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, entity)));

        log.info(LogMessage.RESOURCE_FOUND, entity);

        return dailyActivityLimit;
    }

    private void checkUniqueness(DailyActivityLimitDto request, ActivityType previousActivityType) {
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

    private boolean dailyActivityLimitExistsByActivityType(ActivityType activityType) {
        return dailyActivityLimitRepository.existsByActivityType(activityType);
    }
}
