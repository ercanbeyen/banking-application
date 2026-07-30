package com.ercanbeyen.bankingapplication.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;

import java.io.Serializable;
import java.time.Instant;

@Data
@MappedSuperclass
public sealed class BaseDto implements Serializable permits AccountDto, BranchDto, CustomerDto, ExchangeDto, NewsDto, MoneyTransferOrderDto, TermDepositInterestRateDto {
    private Integer id;
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd HH:mm:ss",
            timezone = "UTC"
    )
    private Instant createdAt;
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd HH:mm:ss",
            timezone = "UTC"
    )
    private Instant updatedAt;
}
