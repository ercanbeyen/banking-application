package com.ercanbeyen.bankingapplication.service;

import com.ercanbeyen.bankingapplication.constant.enums.Currency;
import com.ercanbeyen.bankingapplication.dto.ExchangeDto;
import com.ercanbeyen.bankingapplication.option.ExchangeFilteringOption;
import com.ercanbeyen.bankingapplication.view.entity.ExchangeView;

import java.util.List;

public interface ExchangeService extends BaseService<ExchangeDto, ExchangeFilteringOption> {
    Double convertMoneyBetweenCurrencies(Currency fromCurrency, Currency toCurrency, Double amount);
    List<ExchangeView> getExchangeViews();
    Double getBankExchangeRate(Currency fromCurrency, Currency toCurrency);

}
