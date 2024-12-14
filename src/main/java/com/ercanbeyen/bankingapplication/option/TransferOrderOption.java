package com.ercanbeyen.bankingapplication.option;

import com.ercanbeyen.bankingapplication.constant.enums.PaymentType;
import lombok.Data;

import java.time.LocalDate;

@Data
public non-sealed class TransferOrderOption extends BaseFilteringOption {
    private Integer senderAccountId;
    private Integer receiverAccountId;
    private LocalDate transferDate;
    private PaymentType paymentType;
}
