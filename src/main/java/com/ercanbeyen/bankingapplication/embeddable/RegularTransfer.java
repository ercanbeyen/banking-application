package com.ercanbeyen.bankingapplication.embeddable;

import com.ercanbeyen.bankingapplication.constant.enums.PaymentPeriod;
import com.ercanbeyen.bankingapplication.constant.enums.PaymentType;
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
    PaymentPeriod paymentPeriod;
    Double amount;
    @Enumerated(EnumType.STRING)
    PaymentType paymentType;
    String explanation;
}
