package com.ercanbeyen.bankingapplication.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Data
public non-sealed class MoneyTransferOrderDto extends BaseDto {
    private Integer senderAccountId;
    private Integer chargedAccountId;
    @NotNull(message = "Transfer date should not be null")
    private LocalDate transferDate;
    @Valid
    private RegularMoneyTransferDto regularMoneyTransferDto;
}
