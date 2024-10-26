package com.ercanbeyen.bankingapplication.repository;

import com.ercanbeyen.bankingapplication.constant.enums.Currency;
import com.ercanbeyen.bankingapplication.entity.Fee;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeeRepository extends BaseRepository<Fee> {
    List<Fee> findAllByCurrencyAndDepositPeriod(
            Currency currency,
            int depositPeriod
    );
}
