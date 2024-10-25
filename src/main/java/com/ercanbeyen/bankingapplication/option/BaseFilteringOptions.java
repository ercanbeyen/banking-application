package com.ercanbeyen.bankingapplication.option;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@MappedSuperclass
public sealed class BaseFilteringOptions permits AccountFilteringOptions, BranchFilteringOptions, ChargeFilteringOptions, CustomerFilteringOptions, DailyActivityLimitFilteringOptions, ExchangeFilteringOptions, FeeFilteringOptions, TransferOrderOptions {
    @JsonFormat(pattern="yyyy-MM-dd")
    private LocalDate createdAt;
    @JsonFormat(pattern="yyyy-MM-dd")
    private LocalDate updatedAt;
}
