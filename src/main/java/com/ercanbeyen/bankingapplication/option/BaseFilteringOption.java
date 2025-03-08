package com.ercanbeyen.bankingapplication.option;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@MappedSuperclass
public sealed class BaseFilteringOption permits AccountFilteringOption, BranchFilteringOption, CustomerFilteringOption, DailyActivityLimitFilteringOption, ExchangeFilteringOption, FeeFilteringOption, TransferOrderOption {
    @JsonFormat(pattern="yyyy-MM-dd")
    private LocalDate createdAt;
    @JsonFormat(pattern="yyyy-MM-dd")
    private LocalDate updatedAt;
}
