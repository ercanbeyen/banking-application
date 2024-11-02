package com.ercanbeyen.bankingapplication.service;

import com.ercanbeyen.bankingapplication.dto.SurveyDto;

import java.util.List;
import java.util.UUID;

public interface SurveyService {
    List<SurveyDto> getSurveys();
    SurveyDto getSurvey(UUID id);
    SurveyDto createSurvey(SurveyDto surveyDto);
    SurveyDto updateSurvey(UUID id, SurveyDto surveyDto);
}
