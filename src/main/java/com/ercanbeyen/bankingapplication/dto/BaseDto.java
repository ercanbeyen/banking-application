package com.ercanbeyen.bankingapplication.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@MappedSuperclass
public sealed class BaseDto permits AccountDto, BranchDto, ChargeDto, CustomerDto, DailyActivityLimitDto, ExchangeDto, NewsDto, TransferOrderDto, FeeDto {
    private Integer id;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
