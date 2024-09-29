package com.ercanbeyen.bankingapplication.dto.response;

public record CustomerStatisticsResponse(String customerNationalId, String fullName, Integer accountId, Double balance) {

}
