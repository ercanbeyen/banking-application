package com.ercanbeyen.bankingapplication.entity;

import com.ercanbeyen.bankingapplication.embeddable.RegularTransfer;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
@Table(name = "transfer_orders")
public non-sealed class TransferOrder extends BaseEntity {
    private Integer id;
    @ManyToOne
    private Account senderAccount;
    private LocalDate transferDate;
    @Embedded
    private RegularTransfer regularTransfer;
}
