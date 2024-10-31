package com.ercanbeyen.bankingapplication.repository;

import com.ercanbeyen.bankingapplication.constant.enums.Currency;
import com.ercanbeyen.bankingapplication.entity.Fee;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeeRepository extends BaseRepository<Fee> {
    List<Fee> findAllByCurrencyAndDepositPeriod(
            @Param("currency") Currency currency,
            @Param("depositPeriod") int depositPeriod
    );

    @Query(value = """
            SELECT f
            FROM Fee f
            WHERE f.currency = :currency AND f.depositPeriod = :depositPeriod AND :balance BETWEEN f.minimumAmount AND f.maximumAmount
            """)
    Optional<Fee> findByCurrencyAndDepositPeriodAndBalance(
            @Param("currency") Currency currency,
            @Param("depositPeriod") int depositPeriod,
            @Param("balance") double balance
    );
}
