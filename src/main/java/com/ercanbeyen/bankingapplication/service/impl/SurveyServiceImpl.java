package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.enums.SurveyType;
import com.ercanbeyen.bankingapplication.constant.message.LogMessage;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.dto.AccountActivityDto;
import com.ercanbeyen.bankingapplication.dto.NotificationDto;
import com.ercanbeyen.bankingapplication.dto.SurveyDto;
import com.ercanbeyen.bankingapplication.dto.response.FrequencyStatisticsResponse;
import com.ercanbeyen.bankingapplication.dto.response.SurveyStatisticsResponse;
import com.ercanbeyen.bankingapplication.embeddable.Rating;
import com.ercanbeyen.bankingapplication.entity.Survey;
import com.ercanbeyen.bankingapplication.entity.SurveyCompositeKey;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import com.ercanbeyen.bankingapplication.exception.ResourceExpectationFailedException;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.mapper.SurveyMapper;
import com.ercanbeyen.bankingapplication.option.SurveyFilteringOption;
import com.ercanbeyen.bankingapplication.repository.SurveyRepository;
import com.ercanbeyen.bankingapplication.service.AccountActivityService;
import com.ercanbeyen.bankingapplication.service.CustomerService;
import com.ercanbeyen.bankingapplication.service.NotificationService;
import com.ercanbeyen.bankingapplication.service.SurveyService;
import com.ercanbeyen.bankingapplication.util.LoggingUtil;
import com.ercanbeyen.bankingapplication.util.StatisticsUtil;
import com.ercanbeyen.bankingapplication.util.SurveyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
@Slf4j
public class SurveyServiceImpl implements SurveyService {
    private final SurveyRepository surveyRepository;
    private final SurveyMapper surveyMapper;
    private final CustomerService customerService;
    private final AccountActivityService accountActivityService;
    private final NotificationService notificationService;

    @Override
    public List<SurveyDto> getSurveys(SurveyFilteringOption filteringOption) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Predicate<Survey> surveyPredicate = survey -> {
            SurveyCompositeKey key = survey.getKey();

            boolean customerNationalIdFilter = (Optional.ofNullable(filteringOption.customerNationalId()).isEmpty() || filteringOption.customerNationalId().equals(key.getCustomerNationalId()));
            boolean accountActivityTypeFilter = (Optional.ofNullable(filteringOption.accountActivityType()).isEmpty() || filteringOption.accountActivityType() == survey.getAccountActivityType());
            boolean surveyTypeFilter = (Optional.ofNullable(filteringOption.surveyType()).isEmpty() || filteringOption.surveyType() == key.getSurveyType());
            boolean createdAtFilter = (Optional.ofNullable(filteringOption.createdAt()).isEmpty() || filteringOption.createdAt().isEqual(key.getCreatedAt().toLocalDate()));
            boolean validUntilFilter = (Optional.ofNullable(filteringOption.validUntil()).isEmpty() || filteringOption.validUntil().isEqual(survey.getValidUntil().toLocalDate()));

            return customerNationalIdFilter && accountActivityTypeFilter && surveyTypeFilter && createdAtFilter && validUntilFilter;
        };

        Comparator<Survey> surveyComparator = Comparator.comparing(survey -> survey.getKey().getCreatedAt());
        surveyComparator = surveyComparator.reversed();

        return surveyRepository.findAll()
                .stream()
                .filter(surveyPredicate)
                .sorted(surveyComparator)
                .map(surveyMapper::entityToDto)
                .toList();
    }

    @Override
    public SurveyDto getSurvey(String customerNationalId, String accountActivityId, LocalDateTime createdAt, SurveyType surveyType) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());
        SurveyCompositeKey surveyCompositeKey = new SurveyCompositeKey(customerNationalId, accountActivityId, createdAt, surveyType);
        return surveyMapper.entityToDto(findByKey(surveyCompositeKey));
    }

    @Override
    public SurveyDto createSurvey(SurveyDto request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        checkCustomerAndAccountActivity(request.key().getCustomerNationalId(), request.key().getAccountActivityId());
        AccountActivityDto accountActivityDto = accountActivityService.getAccountActivity(request.key().getAccountActivityId());
        Survey survey = Survey.valueOf(request, accountActivityDto);

        checkExpiration(survey);

        Survey savedSurvey = surveyRepository.save(survey);
        String entity = Entity.SURVEY.getValue();
        log.info(LogMessage.RESOURCE_CREATE_SUCCESS, entity, savedSurvey.getKey());

        NotificationDto notificationDto = new NotificationDto(
                savedSurvey.getKey().getCustomerNationalId(),
                String.format("Please evaluate your %s activity at %s in the %s", savedSurvey.getAccountActivityType().getValue(), accountActivityDto.createdAt(), entity)
        );

        notificationService.createNotification(notificationDto);

        return surveyMapper.entityToDto(savedSurvey);
    }

    @Override
    public SurveyDto updateSurvey(String customerNationalId, String accountActivityId, LocalDateTime createdAt, SurveyType surveyType, SurveyDto request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        SurveyCompositeKey key = new SurveyCompositeKey(customerNationalId, accountActivityId, createdAt, surveyType);
        Survey survey = findByKey(key);

        validateSurvey(survey, request);

        survey.setTitle(request.title());
        survey.setRatings(request.ratings());
        survey.setCustomerSuggestion(request.customerSuggestion());
        survey.setUpdatedAt(LocalDateTime.now());

        return surveyMapper.entityToDto(surveyRepository.save(survey));
    }

    @Override
    public void deleteSurvey(String customerNationalId, String accountActivityId, LocalDateTime createdAt, SurveyType surveyType) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        SurveyCompositeKey key = new SurveyCompositeKey(customerNationalId, accountActivityId, createdAt, surveyType);
        String entity = Entity.SURVEY.getValue();

        surveyRepository.findById(key)
                .ifPresentOrElse(_ -> {
                    log.info(LogMessage.RESOURCE_FOUND, entity);
                    surveyRepository.deleteById(key);
                }, () -> {
                    log.error(LogMessage.RESOURCE_NOT_FOUND, entity);
                    throw new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, entity));
                });

        log.info(LogMessage.RESOURCE_DELETE_SUCCESS, entity, key);
    }

    @Override
    public SurveyDto updateValidationTime(String customerNationalId, String accountActivityId, LocalDateTime createdAt, SurveyType surveyType, LocalDateTime request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        SurveyCompositeKey key = new SurveyCompositeKey(customerNationalId, accountActivityId, createdAt, surveyType);
        Survey survey = findByKey(key);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nearestValidationTime = SurveyUtil.getNearestValidationTime();

        if (request.isBefore(nearestValidationTime)) {
            throw new ResourceConflictException(String.format("Validation time cannot be before %s", nearestValidationTime));
        }

        log.info("Requested validation time is appropriate");

        survey.setUpdatedAt(now);
        survey.setValidUntil(request);

        return surveyMapper.entityToDto(surveyRepository.save(survey));
    }

    @Override
    public SurveyStatisticsResponse<Integer, Integer> getSurveyStatistics(String customerNationalId, String accountActivityId, LocalDateTime createdAt, SurveyType surveyType, Integer minimumFrequency) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        SurveyCompositeKey key = new SurveyCompositeKey(customerNationalId, accountActivityId, createdAt, surveyType);
        Survey survey = findByKey(key);

        if (!survey.getUpdatedAt().isAfter(survey.getKey().getCreatedAt())) {
            throw new ResourceConflictException(String.format("%s must be filled to get the statistics", Entity.SURVEY.getValue()));
        }

        List<Rating> ratings = survey.getRatings();
        List<Integer> rates = ratings
                .stream()
                .map(Rating::getRate)
                .toList();

        FrequencyStatisticsResponse<Integer, Integer> frequencyStatisticsResponse = new FrequencyStatisticsResponse<>(StatisticsUtil.getFrequencies(rates, minimumFrequency));
        Double average = ratings
                .stream()
                .mapToDouble(Rating::getRate)
                .average()
                .orElse(0);

        return new SurveyStatisticsResponse<>(frequencyStatisticsResponse, average);
    }

    private Survey findByKey(SurveyCompositeKey key) {
        checkCustomerAndAccountActivity(key.getCustomerNationalId(), key.getAccountActivityId());
        String entity = Entity.SURVEY.getValue();

        Survey survey = surveyRepository.findById(key)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, entity)));

        log.info(LogMessage.RESOURCE_FOUND, entity);

        return survey;
    }

    private void checkCustomerAndAccountActivity(String customerNationalId, String accountActivityId) {
        String entity = Entity.CUSTOMER.getValue();

        if (!customerService.existsByNationalId(customerNationalId)) {
            log.error(LogMessage.RESOURCE_NOT_FOUND, entity);
            throw new ResourceExpectationFailedException(String.format(ResponseMessage.NOT_FOUND, entity));
        }

        log.info(LogMessage.RESOURCE_FOUND, entity);
        entity = Entity.ACCOUNT_ACTIVITY.getValue();

        if (!accountActivityService.existsByIdAndCustomerNationalId(accountActivityId, customerNationalId)) {
            String customerEntity = Entity.CUSTOMER.getValue();
            log.error(LogMessage.RESOURCE_NOT_FOUND + " in {}", entity, customerEntity);
            throw new ResourceExpectationFailedException(entity + " is not related with " + customerEntity);
        }
    }

    private static void validateSurvey(Survey survey, SurveyDto request) {
        checkExpiration(survey);
        String entity = Entity.SURVEY.getValue();

        /* Rates should not be null after updated by customer */
        for (Rating rating : request.ratings()) {
            if (Optional.ofNullable(rating.getRate()).isEmpty()) {
                log.error("Rate is null in {}", entity);
                throw new ResourceConflictException("Rate cannot be null");
            }
        }

        log.info("Rates are not null in {}", entity);
    }

    private static void checkExpiration(Survey survey) {
        String entity = Entity.SURVEY.getValue();

        if (survey.getValidUntil().isBefore(LocalDateTime.now())) {
            throw new ResourceConflictException(entity + " expired");
        }

        log.info("{} has not expired", entity);
    }
}
