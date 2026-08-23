package com.ercanbeyen.bankingapplication.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@MappedSuperclass
public sealed class BaseDto implements Serializable permits AccountDto, AtmDto, BranchDto, CustomerDto, ExchangeDto, NewsDto, MoneyTransferOrderDto, TermDepositInterestRateDto {
    private Integer id;
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    private LocalDateTime createdAt;
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    private LocalDateTime updatedAt;
}
