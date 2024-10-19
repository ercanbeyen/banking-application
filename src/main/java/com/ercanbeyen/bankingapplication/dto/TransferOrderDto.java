package com.ercanbeyen.bankingapplication.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public non-sealed class TransferOrderDto extends BaseDto {
    private Integer senderAccountId;
    private Integer chargedAccountId;
    @NotNull(message = "Transfer date should not be null")
    private LocalDate transferDate;
    @Valid
    private RegularTransferDto regularTransferDto;
}
