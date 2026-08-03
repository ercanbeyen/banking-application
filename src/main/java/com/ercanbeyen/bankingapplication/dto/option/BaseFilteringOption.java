package com.ercanbeyen.bankingapplication.dto.option;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;

import java.time.LocalDate;

@Data
@MappedSuperclass
public abstract sealed class BaseFilteringOption permits AccountFilteringOption, ChannelFilteringOption, CustomerFilteringOption, ExchangeFilteringOption, MoneyTransferOrderOption, TermDepositInterestRateFilteringOption {
    @JsonFormat(pattern="yyyy-MM-dd")
    private LocalDate createdAt;
    @JsonFormat(pattern="yyyy-MM-dd")
    private LocalDate updatedAt;
}
