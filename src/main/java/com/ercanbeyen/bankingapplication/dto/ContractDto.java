package com.ercanbeyen.bankingapplication.dto;

import java.time.LocalDateTime;

public record ContractDto(String id, String name, String fileId, String customerNationalId, LocalDateTime approvedAt) {

}
