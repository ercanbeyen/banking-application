package com.ercanbeyen.bankingapplication.dto.response;

import java.util.Map;

public record FrequencyStatisticsResponse<T, V>(Map<T, V> frequencyStatistics) {

}
