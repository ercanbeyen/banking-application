package com.ercanbeyen.bankingapplication.entity;

import com.ercanbeyen.bankingapplication.embeddable.Address;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;
import java.util.Optional;

@Data
@Entity
@Table(name = "branches", indexes = {
        @Index(name = "city_and_district_index", columnList = "city, district")
})
public final class Branch extends BaseEntity {
    @Column(unique = true)
    private String name;
    @Embedded
    private Address address;
    @OneToMany(mappedBy = "branch")
    private List<Account> accounts;

    @Override
    public String toString() {
        List<Integer> accountIds = Optional.ofNullable(accounts).isEmpty()
                ? null
                : accounts.stream()
                .map(Account::getId)
                .toList();

        return "Branch{" +
                "name='" + name + '\'' +
                ", address=" + address.getDetails() +
                ", accounts=" + accountIds +
                '}';
    }
}
