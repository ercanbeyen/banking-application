package com.ercanbeyen.bankingapplication.dto;

import com.ercanbeyen.bankingapplication.constant.enums.PaymentPeriod;
import com.ercanbeyen.bankingapplication.constant.enums.PaymentType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RegularTransferDto(
        @NotNull(message = "Transfer order period should not be null")
        PaymentPeriod paymentPeriod,
        @NotNull(message = "receiver account id should not be null")
        Integer receiverAccountId,
        Integer chargedAccountId,
        @NotNull(message = "Amount should not be null")
        @Min(value = 1, message = "Minimum amount should be {value}")
        Double amount,
        @NotNull(message = "Payment type should not be null")
        PaymentType paymentType,
        String explanation) {

}
