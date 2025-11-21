package com.ercanbeyen.bankingapplication.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
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

    @Serial
    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
    }

    @Serial
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
    }
}
