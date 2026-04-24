package com.ercanbeyen.bankingapplication.repository;

import com.ercanbeyen.bankingapplication.constant.enums.Currency;
import com.ercanbeyen.bankingapplication.entity.TermDepositInterestRate;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TermDepositInterestRateRepository extends BaseRepository<TermDepositInterestRate> {
    List<TermDepositInterestRate> findAllByCurrencyAndDepositMaturity(Currency currency, int depositMaturity);

    @Query(value = """
            SELECT t
            FROM TermDepositInterestRate t
            WHERE t.currency = :currency AND t.depositMaturity = :depositMaturity AND :balance BETWEEN t.minimumBalance AND t.maximumBalance
            """)
    Optional<TermDepositInterestRate> findByCurrencyAndDepositMaturityAndBalance(
            @Param("currency") Currency currency,
            @Param("depositMaturity") int depositMaturity,
            @Param("balance") double balance
    );
}
