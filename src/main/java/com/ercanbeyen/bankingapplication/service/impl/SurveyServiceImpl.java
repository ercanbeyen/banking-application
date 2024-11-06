package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.enums.SurveyType;
import com.ercanbeyen.bankingapplication.constant.message.LogMessages;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessages;
import com.ercanbeyen.bankingapplication.dto.SurveyDto;
import com.ercanbeyen.bankingapplication.entity.Survey;
import com.ercanbeyen.bankingapplication.entity.SurveyCompositeKey;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.mapper.SurveyMapper;
import com.ercanbeyen.bankingapplication.repository.SurveyRepository;
import com.ercanbeyen.bankingapplication.service.SurveyService;
import com.ercanbeyen.bankingapplication.util.LoggingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SurveyServiceImpl implements SurveyService {
    private final SurveyRepository surveyRepository;
    private final SurveyMapper surveyMapper;

    @Override
    public List<SurveyDto> getSurveys() {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        return surveyRepository.findAll()
                .stream()
                .map(surveyMapper::entityToDto)
                .toList();
    }

    @Override
    public SurveyDto getSurvey(String customerNationalId, SurveyType surveyType, LocalDate date) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());
        SurveyCompositeKey surveyCompositeKey = new SurveyCompositeKey(customerNationalId, surveyType, date);
        return surveyMapper.entityToDto(findByKey(surveyCompositeKey));
    }

    @Override
    public SurveyDto createSurvey(SurveyDto request) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        Survey survey = Survey.valueOf(request);
        Survey savedSurvey = surveyRepository.save(survey);
        log.info(LogMessages.RESOURCE_CREATE_SUCCESS, Entity.SURVEY.getValue(), savedSurvey.getKey());

        return surveyMapper.entityToDto(savedSurvey);
    }

    @Override
    public SurveyDto updateSurvey(String customerNationalId, SurveyType surveyType, LocalDate date, SurveyDto request) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        SurveyCompositeKey key = new SurveyCompositeKey(customerNationalId, surveyType, date);
        Survey survey = findByKey(key);

        survey.setRatings(request.ratings());
        survey.setCustomerSuggestion(request.customerSuggestion());
        survey.setUpdatedAt(LocalDate.now());
        survey.setValidUntil(request.validUntil());

        Survey savedSurvey = surveyRepository.save(survey);
        log.info(LogMessages.RESOURCE_CREATE_SUCCESS, Entity.SURVEY.getValue(), savedSurvey.getKey());

        return surveyMapper.entityToDto(savedSurvey);
    }

    private Survey findByKey(SurveyCompositeKey key) {
        String entity = Entity.SURVEY.getValue();
        Survey survey = surveyRepository.findById(key)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessages.NOT_FOUND, entity)));

        log.info(LogMessages.RESOURCE_FOUND, entity);

        return survey;
    }
}
