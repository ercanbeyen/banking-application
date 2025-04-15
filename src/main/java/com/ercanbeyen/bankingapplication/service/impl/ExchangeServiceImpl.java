package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.Currency;
import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.LogMessage;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.dto.ExchangeDto;
import com.ercanbeyen.bankingapplication.entity.Exchange;
import com.ercanbeyen.bankingapplication.service.ExchangeService;
import com.ercanbeyen.bankingapplication.view.entity.ExchangeView;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.mapper.ExchangeMapper;
import com.ercanbeyen.bankingapplication.option.ExchangeFilteringOption;
import com.ercanbeyen.bankingapplication.repository.ExchangeRepository;
import com.ercanbeyen.bankingapplication.view.repository.ExchangeViewRepository;
import com.ercanbeyen.bankingapplication.util.ExchangeUtil;
import com.ercanbeyen.bankingapplication.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javatuples.Pair;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeServiceImpl implements ExchangeService {
    private final ExchangeRepository exchangeRepository;
    private final ExchangeViewRepository exchangeViewRepository;
    private final ExchangeMapper exchangeMapper;

    @Override
    public List<ExchangeDto> getEntities(ExchangeFilteringOption filteringOption) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        List<ExchangeDto> exchangeDtos = new ArrayList<>();
        exchangeRepository.findAll()
                .forEach(exchange -> exchangeDtos.add(exchangeMapper.entityToDto(exchange)));

        return exchangeDtos;
    }

    @Override
    public ExchangeDto getEntity(Integer id) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());
        Exchange exchange = findById(id);
        return exchangeMapper.entityToDto(exchange);
    }

    @Override
    public ExchangeDto createEntity(ExchangeDto request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Exchange exchange = exchangeMapper.dtoToEntity(request);
        checkExistsByBaseAndTargetCurrencies(exchange.getBaseCurrency(), exchange.getTargetCurrency());

        Exchange savedExchange = exchangeRepository.save(exchange);
        log.info(LogMessage.RESOURCE_CREATE_SUCCESS, Entity.EXCHANGE.getValue(), savedExchange.getId());

        return exchangeMapper.entityToDto(savedExchange);
    }

    @Override
    public ExchangeDto updateEntity(Integer id, ExchangeDto request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

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
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        String entity = Entity.EXCHANGE.getValue();

        exchangeRepository.findById(id)
                .ifPresentOrElse(exchange -> {
                    log.info(LogMessage.RESOURCE_FOUND, entity);
                    exchangeRepository.deleteById(id);
                }, () -> {
                    log.error(LogMessage.RESOURCE_NOT_FOUND, entity);
                    throw new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, entity));
                });

        log.info(LogMessage.RESOURCE_DELETE_SUCCESS, entity, id);
    }

    @Override
    public Double convertMoneyBetweenCurrencies(Currency fromCurrency, Currency toCurrency, Double amount) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());
        return convertMoneyBetweenCurrenciesWithBankRates(
                fromCurrency,
                toCurrency,
                amount
        );
    }

    @Override
    public List<ExchangeView> getExchangeViews() {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());
        return exchangeViewRepository.findAll();
    }

    @Override
    public Double getBankExchangeRate(Currency fromCurrency, Currency toCurrency) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());
        Pair<Double, Double> exchangeRate = getExchangeRate(fromCurrency, toCurrency);
        return exchangeRate.getValue0();
    }

    private Exchange findById(Integer id) {
        String entity = Entity.EXCHANGE.getValue();
        Exchange exchange = exchangeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, entity)));

        log.info(LogMessage.RESOURCE_FOUND, entity);

        return exchange;
    }

    private double convertMoneyBetweenCurrenciesWithBankRates(Currency baseCurrency, Currency targetCurrency, Double amount) {
        if (baseCurrency == targetCurrency) {
            return amount;
        }

        Pair<Double, Double> currencyFields = getExchangeRate(baseCurrency, targetCurrency);
        double rate = currencyFields.getValue0();
        double exponential = currencyFields.getValue1();
        log.info("Rate and Exponential: {} & {}", rate, exponential);

        double effectOfRate = Math.pow(rate, exponential);
        log.info("Effect of rate: {}", effectOfRate);

        return amount * effectOfRate;
    }

    private Pair<Double, Double> getExchangeRate(Currency baseCurrency, Currency targetCurrency) {
        Optional<ExchangeView> maybeExchangeView = exchangeViewRepository.findByBaseCurrencyAndTargetCurrency(baseCurrency, targetCurrency);
        ExchangeView exchangeView;
        double exponential;
        double rate;

        if (maybeExchangeView.isPresent()) { // Bank sells foreign currency & Customer buys foreign currency
            log.info(LogMessage.RESOURCE_FOUND, Entity.EXCHANGE.getValue());
            exchangeView = maybeExchangeView.get();
            exponential = -1; // foreign currency sell
            rate = exchangeView.getSellRate();

        } else { // Bank buys foreign currency & Customer sells foreign currency
            exchangeView = exchangeViewRepository.findByTargetCurrencyAndBaseCurrency(baseCurrency, targetCurrency)
                    .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, Entity.EXCHANGE.getValue())));
            log.info(LogMessage.RESOURCE_FOUND, "Reverse " + Entity.EXCHANGE.getValue());
            exponential = 1; // foreign currency buy
            rate = exchangeView.getBuyRate();
        }

        log.info("Buy and sell rates: {} {}", exchangeView.getBuyRate(), exchangeView.getSellRate());

        return new Pair<>(rate, exponential);
    }

    private void checkExistsByBaseAndTargetCurrencies(Currency base, Currency target) {
        ExchangeUtil.checkCurrenciesBeforeMoneyExchange(base, target);

        final String logMessage = "Existence of Exchange (Base: {} & Target: {}): {}";
        boolean existsByBaseAndTarget = exchangeRepository.existsByBaseCurrencyAndTargetCurrency(base, target);
        log.info(logMessage, base, target, existsByBaseAndTarget);

        boolean existsByTargetAndBase = exchangeRepository.existsByTargetCurrencyAndBaseCurrency(base, target);
        log.info(logMessage, target, base, existsByTargetAndBase);

        if (existsByBaseAndTarget || existsByTargetAndBase) {
            throw new ResourceConflictException(String.format(ResponseMessage.ALREADY_EXISTS, Entity.EXCHANGE.getValue()));
        }
    }
}
