package com.ercanbeyen.bankingapplication.option;

import lombok.Data;

@Data
public non-sealed class TransferOrderOptions extends BaseFilteringOptions {
    private Integer senderAccountId;
    private Integer receiverAccountId;
}
