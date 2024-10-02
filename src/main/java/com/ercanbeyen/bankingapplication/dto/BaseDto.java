package com.ercanbeyen.bankingapplication.dto;

import jakarta.persistence.MappedSuperclass;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@MappedSuperclass
public sealed class BaseDto permits AccountDto, BranchDto, CustomerDto, ExchangeDto, NewsDto, TransferOrderDto {
    private Integer id;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
