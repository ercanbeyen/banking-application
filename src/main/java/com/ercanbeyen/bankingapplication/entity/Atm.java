package com.ercanbeyen.bankingapplication.entity;

import com.ercanbeyen.bankingapplication.embeddable.Address;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
public non-sealed class Atm extends BaseEntity {
    @Embedded
    private Address address;
}
