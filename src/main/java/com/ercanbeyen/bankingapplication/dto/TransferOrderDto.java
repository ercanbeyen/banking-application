package com.ercanbeyen.bankingapplication.dto;

import com.ercanbeyen.bankingapplication.constant.enums.TransferOrderTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public non-sealed class TransferOrderDto extends BaseDto {
    private Integer senderAccountId;
    @NotNull(message = "Period should not be null")
    private LocalDate date;
    @Valid
    private RegularTransferDto regularTransferDto;
}
