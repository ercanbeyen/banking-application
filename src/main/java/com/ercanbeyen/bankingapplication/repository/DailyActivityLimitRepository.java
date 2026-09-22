package com.ercanbeyen.bankingapplication.repository;

import com.ercanbeyen.bankingapplication.constant.enums.ActivityType;
import com.ercanbeyen.bankingapplication.entity.DailyActivityLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DailyActivityLimitRepository extends JpaRepository<DailyActivityLimit, String> {
    Optional<DailyActivityLimit> findByActivityType(ActivityType activityType);
    boolean existsByActivityType(ActivityType activityType);
    void deleteByActivityType(ActivityType activityType);
}
