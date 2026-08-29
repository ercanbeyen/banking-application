package com.ercanbeyen.bankingapplication.entity;

import com.ercanbeyen.bankingapplication.embeddable.Address;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@MappedSuperclass
public abstract non-sealed class Channel extends BaseEntity {
    @Column(unique = true)
    protected String name;
    @Embedded
    protected Address address;
}
