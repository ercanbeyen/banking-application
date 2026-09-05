package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.LogMessage;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.dto.DailyAccountActivityLimitDto;
import com.ercanbeyen.bankingapplication.entity.DailyAccountActivityLimit;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.mapper.DailyAccountActivityLimitMapper;
import com.ercanbeyen.bankingapplication.repository.DailyAccountActivityLimitRepository;
import com.ercanbeyen.bankingapplication.service.DailyAccountActivityLimitService;
import com.ercanbeyen.bankingapplication.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class DailyAccountActivityLimitServiceImpl implements DailyAccountActivityLimitService {
    private final DailyAccountActivityLimitRepository dailyAccountActivityLimitRepository;
    private final DailyAccountActivityLimitMapper dailyAccountActivityLimitMapper;

    @CacheEvict(value = "dailyAccountActivityLimits", allEntries = true)
    @Override
    public List<DailyAccountActivityLimitDto> getDailyAccountActivityLimits() {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());
        return dailyAccountActivityLimitRepository.findAll()
                .stream()
                .map(dailyAccountActivityLimitMapper::entityToDto)
                .toList();
    }

    @Cacheable(value = "dailyAccountActivityLimits", key = "#a0")
    @Override
    public DailyAccountActivityLimitDto getDailyAccountActivityLimit(AccountActivityType accountActivityType) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());
        return dailyAccountActivityLimitMapper.entityToDto(findByActivityType(accountActivityType));
    }

    @Override
    public DailyAccountActivityLimitDto createDailyAccountActivityLimit(DailyAccountActivityLimitDto request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        checkUniqueness(request, null);

        DailyAccountActivityLimit dailyAccountActivityLimit = dailyAccountActivityLimitMapper.dtoToEntity(request);
        DailyAccountActivityLimit savedDailyAccountActivityLimit = dailyAccountActivityLimitRepository.save(dailyAccountActivityLimit);
        log.info(LogMessage.RESOURCE_CREATE_SUCCESS, Entity.DAILY_ACCOUNT_ACTIVITY_LIMIT.getValue(), savedDailyAccountActivityLimit.getId());

        return dailyAccountActivityLimitMapper.entityToDto(savedDailyAccountActivityLimit);
    }

    @CachePut(value = "dailyAccountActivityLimits", key = "#a0")
    @Override
    public DailyAccountActivityLimitDto updateDailyAccountActivityLimit(AccountActivityType accountActivityType, DailyAccountActivityLimitDto request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        DailyAccountActivityLimit dailyAccountActivityLimit = findByActivityType(accountActivityType);
        checkUniqueness(request, dailyAccountActivityLimit.getAccountActivityType());

        dailyAccountActivityLimit.setLowerLimit(request.lowerLimit());
        dailyAccountActivityLimit.setUpperLimit(request.upperLimit());

        return dailyAccountActivityLimitMapper.entityToDto(dailyAccountActivityLimitRepository.save(dailyAccountActivityLimit));
    }

    @CacheEvict(value = "dailyAccountActivityLimits", key = "#a0")
    @Override
    public void deleteDailyAccountActivityLimit(AccountActivityType accountActivityType) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        String entity = Entity.DAILY_ACCOUNT_ACTIVITY_LIMIT.getValue();

        if (!dailyAccountActivityLimitExistsByActivityType(accountActivityType)) {
            throw new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, entity));
        }

        log.info(LogMessage.RESOURCE_FOUND, entity);

        dailyAccountActivityLimitRepository.deleteByAccountActivityType(accountActivityType);

        log.info(LogMessage.RESOURCE_DELETE_SUCCESS, entity, accountActivityType);
    }


    private DailyAccountActivityLimit findByActivityType(AccountActivityType activityType) {
        String entity = Entity.DAILY_ACCOUNT_ACTIVITY_LIMIT.getValue();
        DailyAccountActivityLimit dailyAccountActivityLimit = dailyAccountActivityLimitRepository.findByAccountActivityType(activityType)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, entity)));

        log.info(LogMessage.RESOURCE_FOUND, entity);

        return dailyAccountActivityLimit;
    }

    private void checkUniqueness(DailyAccountActivityLimitDto request, AccountActivityType previousActivityType) {
        String entity = Entity.DAILY_ACCOUNT_ACTIVITY_LIMIT.getValue();

        if (previousActivityType == request.accountActivityType()) {
            log.warn(LogMessage.NO_ACCOUNT_ACTIVITY_CHANGE, entity);
            return;
        }

        boolean entityExists = dailyAccountActivityLimitExistsByActivityType(request.accountActivityType());

        if (entityExists) {
            throw new ResourceConflictException(String.format(ResponseMessage.ALREADY_EXISTS, entity));
        }

        log.info(LogMessage.RESOURCE_UNIQUE, entity);
    }

    private boolean dailyAccountActivityLimitExistsByActivityType(AccountActivityType activityType) {
        return dailyAccountActivityLimitRepository.existsByAccountActivityType(activityType);
    }
}
