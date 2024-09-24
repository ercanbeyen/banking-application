package com.ercanbeyen.bankingapplication.repository;

import com.ercanbeyen.bankingapplication.constant.enums.Currency;
import com.ercanbeyen.bankingapplication.entity.Exchange;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExchangeRepository extends BaseRepository<Exchange> {
    boolean existsByBaseCurrencyAndTargetCurrency(Currency baseCurrency, Currency targetCurrency);
    boolean existsByTargetCurrencyAndBaseCurrency(Currency targetCurrency, Currency baseCurrency);
    Optional<Exchange> findByBaseCurrencyAndTargetCurrency(Currency baseCurrency, Currency targetCurrency);
    Optional<Exchange> findByTargetCurrencyAndBaseCurrency(Currency targetCurrency, Currency baseCurrency);
}
