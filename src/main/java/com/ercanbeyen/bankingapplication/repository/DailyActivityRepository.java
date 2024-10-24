package com.ercanbeyen.bankingapplication.repository;

import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import com.ercanbeyen.bankingapplication.entity.DailyActivityLimit;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DailyActivityRepository extends BaseRepository<DailyActivityLimit> {
    Optional<DailyActivityLimit> findByActivityType(AccountActivityType activityType);
    boolean existsByActivityType(AccountActivityType activityType);
}
