package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.AccountType;
import com.ercanbeyen.bankingapplication.constant.enums.Currency;
import com.ercanbeyen.bankingapplication.constant.enums.CurrencyOperation;
import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.LogMessages;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessages;
import com.ercanbeyen.bankingapplication.dto.ExchangeDto;
import com.ercanbeyen.bankingapplication.entity.Account;
import com.ercanbeyen.bankingapplication.entity.Exchange;
import com.ercanbeyen.bankingapplication.view.entity.ExchangeView;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.mapper.ExchangeMapper;
import com.ercanbeyen.bankingapplication.option.ExchangeFilteringOptions;
import com.ercanbeyen.bankingapplication.repository.ExchangeRepository;
import com.ercanbeyen.bankingapplication.view.repository.ExchangeViewRepository;
import com.ercanbeyen.bankingapplication.service.BaseService;
import com.ercanbeyen.bankingapplication.util.ExchangeUtils;
import com.ercanbeyen.bankingapplication.util.LoggingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javatuples.Pair;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeService implements BaseService<ExchangeDto, ExchangeFilteringOptions> {
    private final ExchangeRepository exchangeRepository;
    private final ExchangeViewRepository exchangeViewRepository;
    private final ExchangeMapper exchangeMapper;

    @Override
    public List<ExchangeDto> getEntities(ExchangeFilteringOptions options) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        List<ExchangeDto> exchangeDtos = new ArrayList<>();
        exchangeRepository.findAll()
                .forEach(exchange -> exchangeDtos.add(exchangeMapper.entityToDto(exchange)));

        return exchangeDtos;
    }

    @Override
    public ExchangeDto getEntity(Integer id) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());
        Exchange exchange = findById(id);
        return exchangeMapper.entityToDto(exchange);
    }

    @Override
    public ExchangeDto createEntity(ExchangeDto request) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        Exchange exchange = exchangeMapper.dtoToEntity(request);
        checkExistsByBaseAndTargetCurrencies(exchange.getBaseCurrency(), exchange.getTargetCurrency());

        Exchange savedExchange = exchangeRepository.save(exchange);
        log.info(LogMessages.RESOURCE_CREATE_SUCCESS, Entity.EXCHANGE.getValue(), savedExchange.getId());

        return exchangeMapper.entityToDto(savedExchange);
    }

    @Override
    public ExchangeDto updateEntity(Integer id, ExchangeDto request) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        Exchange exchange = findById(id);

        if (exchange.getBaseCurrency() != request.getBaseCurrency() || exchange.getTargetCurrency() != request.getTargetCurrency()) {
            checkExistsByBaseAndTargetCurrencies(request.getBaseCurrency(), request.getTargetCurrency());
            exchange.setTargetCurrency(request.getTargetCurrency());
            exchange.setBaseCurrency(request.getBaseCurrency());
        }

        exchange.setRate(request.getRate());
        exchange.setSellPercentage(request.getSellPercentage());
        exchange.setBuyPercentage(request.getBuyPercentage());

        return exchangeMapper.entityToDto(exchangeRepository.save(exchange));
    }

    @Override
    public void deleteEntity(Integer id) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        if (!exchangeRepository.existsById(id)) {
            throw new ResourceNotFoundException(String.format(ResponseMessages.NOT_FOUND, Entity.EXCHANGE.getValue()));
        }

        log.info(LogMessages.RESOURCE_FOUND, Entity.EXCHANGE.getValue());

        exchangeRepository.deleteById(id);
    }

    public Double convertMoney(Currency fromCurrency, Currency toCurrency, Double amount) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());
        return convertMoneyAmongCurrencies(
                fromCurrency,
                toCurrency,
                amount,
                CurrencyOperation.CONVERSION
        ); // returns the exchanged amount with exchange rate
    }

    public String showExchangeResult(Currency fromCurrency, Currency toCurrency, Double amount) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());
        Double exchangedAmount = convertMoneyAmongCurrencies(
                fromCurrency,
                toCurrency,
                amount,
                CurrencyOperation.EXCHANGE
        );

        return amount + " " + fromCurrency + " is exchanged to " + exchangedAmount + " " + toCurrency;
    }

    public Double exchangeMoney(Account sellerAccount, Account buyerAccount, Double amount) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());
        checkAccountsBeforeMoneyExchange(sellerAccount, buyerAccount);
        return convertMoneyAmongCurrencies(
                sellerAccount.getCurrency(),
                buyerAccount.getCurrency(),
                amount,
                CurrencyOperation.EXCHANGE
        ); // returns the exchanged amount with bank's buy/sell rate
    }

    public List<ExchangeView> getExchangeViews() {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());
        return exchangeViewRepository.findAll();
    }

    private Exchange findById(Integer id) {
        String entity = Entity.EXCHANGE.getValue();
        Exchange exchange = exchangeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessages.NOT_FOUND, entity)));

        log.info(LogMessages.RESOURCE_FOUND, entity);

        return exchange;
    }

    private static void checkAccountsBeforeMoneyExchange(Account sellerAccount, Account buyerAccount) {
        if (buyerAccount.getCurrency() == sellerAccount.getCurrency()) {
            throw new ResourceConflictException(String.format(ResponseMessages.UNPAIRED_CURRENCIES, "different"));
        }

        if (!buyerAccount.getCustomer().getNationalId().equals(sellerAccount.getCustomer().getNationalId())) {
            throw new ResourceConflictException("Money exchange between different customers is allowed");
        }

        BiPredicate<Account, Account> checkAccountTypeForExchange = (seller, buyer) -> seller.getType() == AccountType.CURRENT && buyer.getType() == AccountType.CURRENT;

        if (!checkAccountTypeForExchange.test(buyerAccount, sellerAccount)) {
            throw new ResourceConflictException("Both buyer and seller accounts must be current accounts");
        }
    }

    /***
     *
     * @param baseCurrency is the "from" part
     * @param targetCurrency is the "to" part
     * @param amount is the requested exchange amount with "from" currency
     * @param operation is the operation type
     * @return the result of the processed amount
     */
    private double convertMoneyAmongCurrencies(Currency baseCurrency, Currency targetCurrency, Double amount, CurrencyOperation operation) {
        log.info("{} is from {} to {}", operation, baseCurrency, targetCurrency);

        /* First operation gets the exchange rate (buy or sell rate), second operation gets conversion rate (base exchange rate) */
        Pair<Double, Double> currencyFields = operation == CurrencyOperation.EXCHANGE ? getRateForExchange(baseCurrency, targetCurrency)
                : getRateForConversion(baseCurrency, targetCurrency);

        double rate = currencyFields.getValue0();
        double exponential = currencyFields.getValue1();
        log.info("Rate and Exponential: {} & {}", rate, exponential);

        double effectOfRate = Math.pow(rate, exponential);
        log.info("Effect of rate: {}", effectOfRate);

        return amount * effectOfRate;
    }

    private Pair<Double, Double> getRateForExchange(Currency baseCurrency, Currency targetCurrency) {
        Optional<ExchangeView> maybeExchangeView = exchangeViewRepository.findByBaseCurrencyAndTargetCurrency(baseCurrency, targetCurrency);
        ExchangeView exchangeView;
        double exponential;
        double rate;

        if (maybeExchangeView.isPresent()) { // Bank sells foreign currency & Customer buys foreign currency
            log.info(LogMessages.RESOURCE_FOUND, Entity.EXCHANGE.getValue());
            exchangeView = maybeExchangeView.get();
            exponential = -1; // foreign currency sell
            rate = exchangeView.getSellRate();

        } else { // Bank buys foreign currency & Customer sells foreign currency
            exchangeView = exchangeViewRepository.findByTargetCurrencyAndBaseCurrency(baseCurrency, targetCurrency)
                    .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessages.NOT_FOUND, Entity.EXCHANGE.getValue())));
            log.info(LogMessages.RESOURCE_FOUND, "Reverse " + Entity.EXCHANGE.getValue());
            exponential = 1; // foreign currency buy
            rate = exchangeView.getBuyRate();
        }

        log.info("Buy and sell rates: {} {}", exchangeView.getBuyRate(), exchangeView.getSellRate());

        return new Pair<>(rate, exponential);
    }

    private Pair<Double, Double> getRateForConversion(Currency baseCurrency, Currency targetCurrency) {
        if (baseCurrency == targetCurrency) {
            return new Pair<>(1D, 1D);
        }

        Optional<Exchange> maybeExchange = exchangeRepository.findByBaseCurrencyAndTargetCurrency(baseCurrency, targetCurrency);
        Exchange exchange;
        double exponential;
        double rate;

        if (maybeExchange.isPresent()) { //// exchange is present
            log.info(LogMessages.RESOURCE_FOUND, Entity.EXCHANGE.getValue());
            exchange = maybeExchange.get();
            exponential = -1; // straight exchange operation
        } else { // reverse exchange is present
            log.info(LogMessages.RESOURCE_FOUND, "Reverse " + Entity.EXCHANGE.getValue());
            exchange = exchangeRepository.findByTargetCurrencyAndBaseCurrency(baseCurrency, targetCurrency)
                    .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessages.NOT_FOUND, Entity.EXCHANGE.getValue())));
            exponential = 1; // reverse exchange operation
        }

        rate = exchange.getRate();
        log.info("Rate: {}", rate);

        return new Pair<>(rate, exponential);
    }

    private void checkExistsByBaseAndTargetCurrencies(Currency base, Currency target) {
        ExchangeUtils.checkCurrencies(base, target);

        final String logMessage = "Existence of Exchange (Base: {} & Target: {}): {}";
        boolean existsByBaseAndTarget = exchangeRepository.existsByBaseCurrencyAndTargetCurrency(base, target);
        log.info(logMessage, base, target, existsByBaseAndTarget);

        boolean existsByTargetAndBase = exchangeRepository.existsByTargetCurrencyAndBaseCurrency(base, target);
        log.info(logMessage, target, base, existsByTargetAndBase);

        if (existsByBaseAndTarget || existsByTargetAndBase) {
            throw new ResourceConflictException(String.format(ResponseMessages.ALREADY_EXISTS, Entity.EXCHANGE.getValue()));
        }
    }
}
