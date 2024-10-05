package com.ercanbeyen.bankingapplication.option;

import com.ercanbeyen.bankingapplication.constant.enums.PaymentType;
import lombok.Data;

import java.time.LocalDate;

@Data
public non-sealed class TransferOrderOptions extends BaseFilteringOptions {
    private Integer senderAccountId;
    private Integer receiverAccountId;
    private LocalDate transferDate;
    private PaymentType paymentType;
}
