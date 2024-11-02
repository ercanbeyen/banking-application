package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.LogMessages;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessages;
import com.ercanbeyen.bankingapplication.dto.SurveyDto;
import com.ercanbeyen.bankingapplication.entity.Survey;
import com.ercanbeyen.bankingapplication.exception.ResourceExpectationFailedException;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.mapper.SurveyMapper;
import com.ercanbeyen.bankingapplication.repository.SurveyRepository;
import com.ercanbeyen.bankingapplication.service.SurveyService;
import com.ercanbeyen.bankingapplication.util.LoggingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SurveyServiceImpl implements SurveyService {
    private final SurveyRepository surveyRepository;
    private final SurveyMapper surveyMapper;
    private final CustomerService customerService;

    @Override
    public List<SurveyDto> getSurveys() {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        return surveyRepository.findAll()
                .stream()
                .map(surveyMapper::entityToDto)
                .toList();
    }

    @Override
    public SurveyDto getSurvey(UUID id) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());
        return surveyMapper.entityToDto(findById(id));
    }

    @Override
    public SurveyDto createSurvey(SurveyDto surveyDto) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        checkSurveyBeforeCreate(surveyDto);

        Survey survey = surveyMapper.dtoToEntity(surveyDto);
        survey.setId(UUID.randomUUID());
        survey.setCustomerSuggestion(surveyDto.customerSuggestion());

        LocalDateTime now = LocalDateTime.now();
        survey.setCreatedAt(now);
        survey.setUpdatedAt(now);
        survey.setYear(now.getYear());


        log.info(LogMessages.RESOURCE_FOUND, Entity.CUSTOMER.getValue());

        Survey savedSurvey = surveyRepository.save(survey);
        log.info(LogMessages.RESOURCE_CREATE_SUCCESS, Entity.SURVEY.getValue(), savedSurvey.getId());

        return surveyMapper.entityToDto(savedSurvey);
    }

    @Override
    public SurveyDto updateSurvey(UUID id, SurveyDto surveyDto) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        Survey survey = findById(id);

        survey.setRatings(surveyDto.ratings());
        survey.setCustomerSuggestion(surveyDto.customerSuggestion());
        survey.setUpdatedAt(LocalDateTime.now());

        Survey savedSurvey = surveyRepository.save(survey);
        log.info(LogMessages.RESOURCE_CREATE_SUCCESS, Entity.SURVEY.getValue(), savedSurvey.getId());

        return surveyMapper.entityToDto(savedSurvey);
    }

    private Survey findById(UUID id) {
        String entity = Entity.SURVEY.getValue();
        Survey survey = surveyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessages.NOT_FOUND, entity)));

        log.info(LogMessages.RESOURCE_FOUND, entity);

        return survey;
    }

    private void checkSurveyBeforeCreate(SurveyDto surveyDto) {
        if (!customerService.existsByNationalId(surveyDto.customerNationalId())) {
            log.error(LogMessages.RESOURCE_NOT_FOUND, Entity.CUSTOMER.getValue());
            throw new ResourceExpectationFailedException("Customer national id is not in database");
        }

        int currentYear = LocalDateTime.now().getYear();

        if (surveyRepository.findByYearAndCustomerNationalId(currentYear, surveyDto.customerNationalId()).isPresent()) {
            log.error(LogMessages.RESOURCE_FOUND, Entity.SURVEY.getValue());
            throw new ResourceExpectationFailedException(String.format("Customer is already rated in %d", currentYear));
        }
    }
}
