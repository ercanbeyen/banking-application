package com.ercanbeyen.bankingapplication.embeddable;

import com.ercanbeyen.bankingapplication.constant.enums.TransferOrderPeriod;
import com.ercanbeyen.bankingapplication.entity.Account;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Embeddable
@AllArgsConstructor
@NoArgsConstructor
public class RegularTransfer {
    @ManyToOne
    @JoinColumn(name = "receiver_account_id", referencedColumnName = "id")
    Account receiverAccount;
    @Enumerated(EnumType.STRING)
    TransferOrderPeriod orderPeriod;
    Double amount;
    String explanation;
}
