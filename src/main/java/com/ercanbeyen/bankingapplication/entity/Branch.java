package com.ercanbeyen.bankingapplication.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Optional;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "branches")
public non-sealed class Branch extends Channel {
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
