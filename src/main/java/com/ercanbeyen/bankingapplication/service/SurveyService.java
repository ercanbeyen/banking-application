package com.ercanbeyen.bankingapplication.service;

import com.ercanbeyen.bankingapplication.constant.enums.SurveyType;
import com.ercanbeyen.bankingapplication.dto.SurveyDto;

import java.time.LocalDate;
import java.util.List;

public interface SurveyService {
    List<SurveyDto> getSurveys();
    SurveyDto getSurvey(String customerNationalId, SurveyType surveyType, LocalDate date);
    SurveyDto createSurvey(SurveyDto surveyDto);
    SurveyDto updateSurvey(String customerNationalId, SurveyType surveyType, LocalDate date, SurveyDto surveyDto);
}
