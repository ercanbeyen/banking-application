package com.ercanbeyen.bankingapplication.entity;

import com.ercanbeyen.bankingapplication.embeddable.RegularMoneyTransfer;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "money_transfer_orders")
public non-sealed class MoneyTransferOrder extends BaseEntity {
    private Integer id;
    @ManyToOne
    private Account senderAccount;
    @ManyToOne
    private Account chargedAccount;
    private LocalDate transferDate;
    @Embedded
    private RegularMoneyTransfer regularMoneyTransfer;
}
