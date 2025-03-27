package com.ercanbeyen.bankingapplication.repository;

import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import com.ercanbeyen.bankingapplication.entity.Charge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChargeRepository extends JpaRepository<Charge, String> {
    Optional<Charge> findByActivityType(AccountActivityType activityType);
    boolean existsByActivityType(AccountActivityType activityType);
    void deleteByActivityType(AccountActivityType activityType);
}
