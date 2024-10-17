package com.ercanbeyen.bankingapplication.option;

import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@MappedSuperclass
public sealed class BaseFilteringOptions permits AccountFilteringOptions, BranchFilteringOptions, ChargeFilteringOptions, CustomerFilteringOptions, ExchangeFilteringOptions, TransferOrderOptions {
    private LocalDateTime createdAt;
}
