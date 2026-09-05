package com.ercanbeyen.bankingapplication.repository;

import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import com.ercanbeyen.bankingapplication.entity.DailyAccountActivityLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DailyAccountActivityLimitRepository extends JpaRepository<DailyAccountActivityLimit, String> {
    Optional<DailyAccountActivityLimit> findByAccountActivityType(AccountActivityType accountActivityType);
    boolean existsByAccountActivityType(AccountActivityType accountActivityType);
    void deleteByAccountActivityType(AccountActivityType accountActivityType);
}
