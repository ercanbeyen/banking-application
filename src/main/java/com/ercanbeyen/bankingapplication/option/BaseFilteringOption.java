package com.ercanbeyen.bankingapplication.option;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;

import java.time.LocalDate;

@Data
@MappedSuperclass
public sealed class BaseFilteringOption permits AccountFilteringOption, BranchFilteringOption, CustomerFilteringOption, ExchangeFilteringOption, FeeFilteringOption, MoneyTransferOrderOption {
    @JsonFormat(pattern="yyyy-MM-dd")
    private LocalDate createdAt;
    @JsonFormat(pattern="yyyy-MM-dd")
    private LocalDate updatedAt;
}
