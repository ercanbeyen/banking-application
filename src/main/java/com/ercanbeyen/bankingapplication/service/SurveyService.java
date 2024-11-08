package com.ercanbeyen.bankingapplication.service;

import com.ercanbeyen.bankingapplication.constant.enums.SurveyType;
import com.ercanbeyen.bankingapplication.dto.SurveyDto;

import java.time.LocalDateTime;
import java.util.List;

public interface SurveyService {
    List<SurveyDto> getSurveys();
    SurveyDto getSurvey(String customerNationalId, String accountActivityId, LocalDateTime eventAt, SurveyType surveyType);
    SurveyDto createSurvey(SurveyDto request);
    SurveyDto updateSurvey(String customerNationalId, String accountActivityId, LocalDateTime createdAt, SurveyType surveyType, SurveyDto request);
    void deleteSurvey(String customerNationalId, String accountActivityId, LocalDateTime createdAt, SurveyType surveyType);
}
