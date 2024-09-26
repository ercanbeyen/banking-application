package com.ercanbeyen.bankingapplication.entity;

import com.ercanbeyen.bankingapplication.constant.enums.City;
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
    @Enumerated(EnumType.STRING)
    private City city;
    private String district;
    @Column(unique = true)
    private String name;
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
                "city=" + city +
                ", district='" + district + '\'' +
                ", name='" + name + '\'' +
                ", accounts=" + accountIds +
                '}';
    }
}
