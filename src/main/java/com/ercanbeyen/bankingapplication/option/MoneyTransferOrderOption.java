package com.ercanbeyen.bankingapplication.option;

import com.ercanbeyen.bankingapplication.constant.enums.PaymentType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Data
public non-sealed class MoneyTransferOrderOption extends BaseFilteringOption {
    private Integer senderAccountId;
    private Integer recipientAccountId;
    private LocalDate transferDate;
    private PaymentType paymentType;
}
