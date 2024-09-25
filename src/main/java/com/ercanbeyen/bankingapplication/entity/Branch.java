package com.ercanbeyen.bankingapplication.entity;

import com.ercanbeyen.bankingapplication.constant.enums.City;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

import java.util.List;
import java.util.Optional;

@Data
@Entity
@Table(name = "branches")
public final class Branch extends BaseEntity {
    private City city;
    private String district;
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
