package com.ercanbeyen.bankingapplication.dto;


import java.time.LocalDateTime;

public record CustomerAgreementDto(String id, String customerNationalId, String agreementTitle, LocalDateTime approvedAt) {

}
