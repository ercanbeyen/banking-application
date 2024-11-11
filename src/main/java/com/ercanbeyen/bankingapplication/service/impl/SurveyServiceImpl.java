package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.enums.SurveyType;
import com.ercanbeyen.bankingapplication.constant.message.LogMessages;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessages;
import com.ercanbeyen.bankingapplication.dto.AccountActivityDto;
import com.ercanbeyen.bankingapplication.dto.SurveyDto;
import com.ercanbeyen.bankingapplication.entity.Survey;
import com.ercanbeyen.bankingapplication.entity.SurveyCompositeKey;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import com.ercanbeyen.bankingapplication.exception.ResourceExpectationFailedException;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.mapper.SurveyMapper;
import com.ercanbeyen.bankingapplication.option.SurveyFilteringOptions;
import com.ercanbeyen.bankingapplication.repository.SurveyRepository;
import com.ercanbeyen.bankingapplication.service.AccountActivityService;
import com.ercanbeyen.bankingapplication.service.SurveyService;
import com.ercanbeyen.bankingapplication.util.LoggingUtils;
import com.ercanbeyen.bankingapplication.util.SurveyUtils;
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

    @Override
    public List<SurveyDto> getSurveys(SurveyFilteringOptions filteringOptions) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        Predicate<Survey> surveyPredicate = survey -> {
            SurveyCompositeKey key = survey.getKey();

            boolean customerNationalIdFilter = (Optional.ofNullable(filteringOptions.customerNationalId()).isEmpty() || filteringOptions.customerNationalId().equals(key.getCustomerNationalId()));
            boolean accountActivityTypeFilter = (Optional.ofNullable(filteringOptions.accountActivityType()).isEmpty() || filteringOptions.accountActivityType() == survey.getAccountActivityType());
            boolean surveyTypeFilter = (Optional.ofNullable(filteringOptions.surveyType()).isEmpty() || filteringOptions.surveyType() == key.getSurveyType());
            boolean createdAtFilter = (Optional.ofNullable(filteringOptions.createdAt()).isEmpty() || filteringOptions.createdAt().isEqual(key.getCreatedAt().toLocalDate()));
            boolean validUntilFilter = (Optional.ofNullable(filteringOptions.validUntil()).isEmpty() || filteringOptions.validUntil().isEqual(survey.getValidUntil().toLocalDate()));

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
    public SurveyDto getSurvey(String customerNationalId, String accountActivityId, LocalDateTime eventAt, SurveyType surveyType) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());
        SurveyCompositeKey surveyCompositeKey = new SurveyCompositeKey(customerNationalId, accountActivityId, eventAt, surveyType);
        return surveyMapper.entityToDto(findByKey(surveyCompositeKey));
    }

    @Override
    public SurveyDto createSurvey(SurveyDto request) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        checkCustomerAndAccountActivity(request.key().getCustomerNationalId(), request.key().getAccountActivityId());
        AccountActivityDto accountActivityDto = accountActivityService.getAccountActivity(request.key().getAccountActivityId());
        Survey survey = Survey.valueOf(request, accountActivityDto);

        validateSurvey(survey);

        Survey savedSurvey = surveyRepository.save(survey);
        log.info(LogMessages.RESOURCE_CREATE_SUCCESS, Entity.SURVEY.getValue(), savedSurvey.getKey());

        return surveyMapper.entityToDto(savedSurvey);
    }

    @Override
    public SurveyDto updateSurvey(String customerNationalId, String accountActivityId, LocalDateTime createdAt, SurveyType surveyType, SurveyDto request) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        SurveyCompositeKey key = new SurveyCompositeKey(customerNationalId, accountActivityId, createdAt, surveyType);
        Survey survey = findByKey(key);

        validateSurvey(survey);

        survey.setTitle(request.title());
        survey.setRatings(request.ratings());
        survey.setCustomerSuggestion(request.customerSuggestion());
        survey.setUpdatedAt(LocalDateTime.now());

        return surveyMapper.entityToDto(surveyRepository.save(survey));
    }

    @Override
    public void deleteSurvey(String customerNationalId, String accountActivityId, LocalDateTime createdAt, SurveyType surveyType) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        SurveyCompositeKey key = new SurveyCompositeKey(customerNationalId, accountActivityId, createdAt, surveyType);
        String entity = Entity.SURVEY.getValue();

        surveyRepository.findById(key)
                .ifPresentOrElse(survey -> {
                    log.info(LogMessages.RESOURCE_FOUND, entity);
                    surveyRepository.deleteById(key);
                }, () -> {
                    log.error(LogMessages.RESOURCE_NOT_FOUND, entity);
                    throw new ResourceNotFoundException(String.format(ResponseMessages.NOT_FOUND, entity));
                });

        log.info(LogMessages.RESOURCE_DELETE_SUCCESS, entity, key);
    }

    @Override
    public SurveyDto updateValidationTime(String customerNationalId, String accountActivityId, LocalDateTime createdAt, SurveyType surveyType, LocalDateTime request) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        SurveyCompositeKey key = new SurveyCompositeKey(customerNationalId, accountActivityId, createdAt, surveyType);
        Survey survey = findByKey(key);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nearestValidationTime = SurveyUtils.getNearestValidationTime();

        if (request.isBefore(nearestValidationTime)) {
            throw new ResourceConflictException(String.format("Validation time cannot be before %s", nearestValidationTime));
        }

        log.info("Requested validation time is appropriate");

        survey.setUpdatedAt(now);
        survey.setValidUntil(request);

        return surveyMapper.entityToDto(surveyRepository.save(survey));
    }

    private Survey findByKey(SurveyCompositeKey key) {
        checkCustomerAndAccountActivity(key.getCustomerNationalId(), key.getAccountActivityId());
        String entity = Entity.SURVEY.getValue();

        Survey survey = surveyRepository.findById(key)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessages.NOT_FOUND, entity)));

        log.info(LogMessages.RESOURCE_FOUND, entity);

        return survey;
    }

    private void checkCustomerAndAccountActivity(String customerNationalId, String accountActivityId) {
        String entity = Entity.CUSTOMER.getValue();

        if (!customerService.existsByNationalId(customerNationalId)) {
            log.error(LogMessages.RESOURCE_NOT_FOUND, entity);
            throw new ResourceExpectationFailedException(String.format(ResponseMessages.NOT_FOUND, entity));
        }

        log.info(LogMessages.RESOURCE_FOUND, entity);
        entity = Entity.ACCOUNT_ACTIVITY.getValue();

        if (!accountActivityService.existsByIdAndCustomerNationalId(accountActivityId, customerNationalId)) {
            String customerEntity = Entity.CUSTOMER.getValue();
            log.error(LogMessages.RESOURCE_NOT_FOUND + " in {}", entity, customerEntity);
            throw new ResourceExpectationFailedException(entity + " is not related with " + customerEntity);
        }
    }

    private static void validateSurvey(Survey survey) {
        String entity = Entity.SURVEY.getValue();

        if (survey.getValidUntil().isBefore(LocalDateTime.now())) {
            throw new ResourceConflictException(entity + " expired");
        }

        log.info("{} is valid", entity);
    }
}
