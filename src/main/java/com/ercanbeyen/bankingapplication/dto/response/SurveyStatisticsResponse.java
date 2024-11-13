package com.ercanbeyen.bankingapplication.dto.response;

public record SurveyStatisticsResponse<T, V>(FrequencyStatisticsResponse<T, V> frequencyStatisticsResponse, Double average) {

}
