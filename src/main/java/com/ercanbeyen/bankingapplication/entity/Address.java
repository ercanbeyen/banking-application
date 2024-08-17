package com.ercanbeyen.bankingapplication.entity;


import com.ercanbeyen.bankingapplication.constant.enums.AddressType;
import com.ercanbeyen.bankingapplication.constant.enums.City;
import com.ercanbeyen.bankingapplication.constant.enums.Ownership;
import jakarta.persistence.*;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "addresses")
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @Enumerated(EnumType.STRING)
    private AddressType type;
    private String phoneNumber;
    @Enumerated(EnumType.STRING)
    private City city;
    private Integer zipCode;
    private String details;
    @Enumerated(EnumType.STRING)
    private Ownership ownership;
    @ManyToMany(mappedBy = "addresses")
    private Set<Customer> customers = new HashSet<>();
}
