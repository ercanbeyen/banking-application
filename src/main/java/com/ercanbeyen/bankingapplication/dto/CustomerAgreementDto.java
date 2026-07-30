package com.ercanbeyen.bankingapplication.dto;


import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

public record CustomerAgreementDto(
        String id,
        String customerNationalId,
        String agreementTitle,
        @JsonFormat(
                shape = JsonFormat.Shape.STRING,
                pattern = "yyyy-MM-dd HH:mm:ss",
                timezone = "UTC"
        )
        Instant approvedAt) {

}
