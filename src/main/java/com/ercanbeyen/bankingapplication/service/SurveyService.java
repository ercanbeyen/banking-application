package com.ercanbeyen.bankingapplication.service;

import com.ercanbeyen.bankingapplication.constant.enums.SurveyType;
import com.ercanbeyen.bankingapplication.dto.SurveyDto;
import com.ercanbeyen.bankingapplication.dto.response.SurveyStatisticsResponse;
import com.ercanbeyen.bankingapplication.dto.option.SurveyFilteringOption;

import java.time.LocalDate;
import java.util.List;

public interface SurveyService {
    List<SurveyDto> getSurveys(SurveyFilteringOption filteringOption);
    SurveyDto getSurvey(String customerNationalId, String accountActivityId, SurveyType surveyType);
    SurveyDto createSurvey(SurveyDto request);
    SurveyDto updateSurvey(String customerNationalId, String accountActivityId, SurveyType surveyType, SurveyDto request);
    void deleteSurvey(String customerNationalId, String accountActivityId, SurveyType surveyType);
    String fillOutSurvey(String customerNationalId, String accountActivityId, SurveyType surveyType, SurveyDto request);
    SurveyStatisticsResponse<Integer, Integer> getSurveyStatistics(String customerNationalId, String accountActivityId, LocalDate createdDate, SurveyType surveyType, Integer minimumFrequency);
}
