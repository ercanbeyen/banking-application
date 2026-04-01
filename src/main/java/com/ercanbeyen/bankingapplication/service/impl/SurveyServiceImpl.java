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
import com.ercanbeyen.bankingapplication.model.Survey;
import com.ercanbeyen.bankingapplication.model.SurveyCompositeKey;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import com.ercanbeyen.bankingapplication.exception.ResourceExpectationFailedException;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.mapper.SurveyMapper;
import com.ercanbeyen.bankingapplication.dto.option.SurveyFilteringOption;
import com.ercanbeyen.bankingapplication.repository.SurveyRepository;
import com.ercanbeyen.bankingapplication.service.AccountActivityService;
import com.ercanbeyen.bankingapplication.service.CustomerService;
import com.ercanbeyen.bankingapplication.service.NotificationService;
import com.ercanbeyen.bankingapplication.service.SurveyService;
import com.ercanbeyen.bankingapplication.util.LoggingUtil;
import com.ercanbeyen.bankingapplication.util.StatisticsUtil;
import com.ercanbeyen.bankingapplication.util.TimeUtil;
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

        SurveyCompositeKey requestedKey = request.key();

        checkCustomerAndAccountActivity(requestedKey.getCustomerNationalId(), requestedKey.getAccountActivityId());

        AccountActivityDto requestedAccountActivity = accountActivityService.getAccountActivity(requestedKey.getAccountActivityId());
        LocalDateTime now = TimeUtil.getTurkeyDateTime();

        SurveyCompositeKey key = new SurveyCompositeKey(
                requestedKey.getCustomerNationalId(),
                requestedAccountActivity.id(),
                now,
                requestedKey.getSurveyType()
        );

        request.ratings().forEach(rating -> rating.setRate(null)); // Reset the rates

        Survey survey = Survey.builder()
                .key(key)
                .title(request.title())
                .validUntil(request.validUntil())
                .updatedAt(now)
                .accountActivityType(requestedAccountActivity.type())
                .ratings(request.ratings())
                .build();

        Survey savedSurvey = surveyRepository.save(survey);

        log.info(LogMessage.RESOURCE_CREATE_SUCCESS, Entity.SURVEY.getValue(), key);

        NotificationDto notificationDto = new NotificationDto(
                survey.getKey().getCustomerNationalId(),
                String.format(ResponseMessage.EVALUATION_MESSAGE, survey.getAccountActivityType().getValue(), requestedAccountActivity.createdAt().toLocalDate(), Entity.SURVEY.getValue(), survey.getValidUntil())
        );

        notificationService.sendNotification(notificationDto);

        return surveyMapper.entityToDto(savedSurvey);
    }

    @Override
    public SurveyDto updateSurvey(String customerNationalId, String accountActivityId, LocalDateTime createdAt, SurveyType surveyType, SurveyDto request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        SurveyCompositeKey key = new SurveyCompositeKey(customerNationalId, accountActivityId, createdAt, surveyType);
        Survey survey = findByKey(key);

        AccountActivityDto requestedAccountActivity = accountActivityService.getAccountActivity(request.key().getAccountActivityId());

        request.ratings().forEach(rating -> rating.setRate(null)); // Reset the rates

        survey.setTitle(request.title());
        survey.setRatings(request.ratings());
        survey.setValidUntil(request.validUntil());
        survey.setUpdatedAt(TimeUtil.getTurkeyDateTime());

        NotificationDto notificationDto = new NotificationDto(
                survey.getKey().getCustomerNationalId(),
                String.format(ResponseMessage.EVALUATION_MESSAGE, survey.getAccountActivityType().getValue(), requestedAccountActivity.createdAt().toLocalDate(), Entity.SURVEY.getValue(), survey.getValidUntil())
        );

        notificationService.sendNotification(notificationDto);

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
    public String fillOutSurvey(String customerNationalId, String accountActivityId, LocalDateTime createdAt, SurveyType surveyType, SurveyDto request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        SurveyCompositeKey key = new SurveyCompositeKey(customerNationalId, accountActivityId, createdAt, surveyType);
        Survey survey = findByKey(key);

        if (survey.getValidUntil().isBefore(TimeUtil.getTurkeyDateTime())) {
            throw new ResourceConflictException(Entity.SURVEY.getValue() + " has expired");
        }

        /* Fill the rates */
        for (int i = 0; i < survey.getRatings().size(); i++) {
            Rating rating = survey.getRatings().get(i);
            Integer rate = request.ratings().get(i).getRate();
            rating.setRate(rate);
        }

        survey.setCustomerSuggestion(request.customerSuggestion());

        return "Thank you for participating in the survey";
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
        List<Integer> rates = ratings.stream()
                .map(Rating::getRate)
                .toList();

        FrequencyStatisticsResponse<Integer, Integer> frequencyStatisticsResponse = new FrequencyStatisticsResponse<>(StatisticsUtil.getFrequencies(rates, minimumFrequency));
        Double average = ratings.stream()
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
        String customerEntity = Entity.CUSTOMER.getValue();
        String accountActivityEntity = Entity.ACCOUNT_ACTIVITY.getValue();

        if (!customerService.existsByNationalId(customerNationalId)) {
            throw new ResourceExpectationFailedException(String.format(ResponseMessage.NOT_FOUND, customerEntity));
        }

        log.info(LogMessage.RESOURCE_FOUND, customerEntity);

        if (!accountActivityService.existsByIdAndCustomerNationalId(accountActivityId, customerNationalId)) {
            throw new ResourceExpectationFailedException(accountActivityEntity + " is not related with " + customerEntity);
        }

        log.info(LogMessage.RESOURCE_FOUND, accountActivityEntity);
    }
}
