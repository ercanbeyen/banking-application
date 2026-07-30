package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.Currency;
import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.LogMessage;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.dto.TermDepositInterestRateDto;
import com.ercanbeyen.bankingapplication.entity.TermDepositInterestRate;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.mapper.TermDepositInterestRateMapper;
import com.ercanbeyen.bankingapplication.dto.option.TermDepositInterestRateFilteringOption;
import com.ercanbeyen.bankingapplication.repository.TermDepositInterestRateRepository;
import com.ercanbeyen.bankingapplication.service.TermDepositInterestRateService;
import com.ercanbeyen.bankingapplication.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

@Slf4j
@RequiredArgsConstructor
@Service
public class TermDepositInterestRateServiceImpl implements TermDepositInterestRateService {
    private final TermDepositInterestRateRepository termDepositInterestRateRepository;
    private final TermDepositInterestRateMapper termDepositInterestRateMapper;

    @Override
    public List<TermDepositInterestRateDto> getEntities(TermDepositInterestRateFilteringOption filteringOption) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Predicate<TermDepositInterestRate> termDepositInterestRatePredicate = termDepositInterestRate -> {
            boolean currencyFilter = (Optional.ofNullable(filteringOption.getCurrency()).isEmpty() || termDepositInterestRate.getCurrency() == filteringOption.getCurrency());
            boolean depositMaturityFilter = (Optional.ofNullable(filteringOption.getDepositMaturity()).isEmpty() || termDepositInterestRate.getDepositMaturity() == filteringOption.getDepositMaturity().intValue());
            boolean updatedAtFilter = (Optional.ofNullable(filteringOption.getUpdatedAt()).isEmpty() || LocalDate.ofInstant(termDepositInterestRate.getUpdatedAt(), ZoneId.systemDefault()).isEqual(filteringOption.getUpdatedAt()));

            return currencyFilter && depositMaturityFilter && updatedAtFilter;
        };

        Comparator<TermDepositInterestRate> termDepositInterestRateComparator = Comparator.comparing(TermDepositInterestRate::getUpdatedAt).reversed();

        return termDepositInterestRateRepository.findAll()
                .stream()
                .filter(termDepositInterestRatePredicate)
                .sorted(termDepositInterestRateComparator)
                .map(termDepositInterestRateMapper::entityToDto)
                .toList();
    }

    @Override
    public TermDepositInterestRateDto getEntity(Integer id) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());
        return termDepositInterestRateMapper.entityToDto(findById(id));
    }

    @Override
    public TermDepositInterestRateDto createEntity(TermDepositInterestRateDto request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        checkUniqueness(request, null);
        TermDepositInterestRate termDepositInterestRate = termDepositInterestRateMapper.dtoToEntity(request);

        TermDepositInterestRate savedTermDepositInterestRate = termDepositInterestRateRepository.save(termDepositInterestRate);
        log.info(LogMessage.RESOURCE_CREATE_SUCCESS, Entity.TERM_DEPOSIT_INTEREST_RATE.getValue(), savedTermDepositInterestRate.getId());

        return termDepositInterestRateMapper.entityToDto(savedTermDepositInterestRate);
    }

    @Override
    public TermDepositInterestRateDto updateEntity(Integer id, TermDepositInterestRateDto request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        TermDepositInterestRate termDepositInterestRate = findById(id);
        checkUniqueness(request, termDepositInterestRate);

        termDepositInterestRate.setCurrency(request.getCurrency());
        termDepositInterestRate.setMinimumBalance(request.getMinimumBalance());
        termDepositInterestRate.setMaximumBalance(request.getMaximumBalance());
        termDepositInterestRate.setDepositMaturity(request.getDepositMaturity());
        termDepositInterestRate.setInterestRate(request.getInterestRate());

        return termDepositInterestRateMapper.entityToDto(termDepositInterestRateRepository.save(termDepositInterestRate));
    }

    @Override
    public void deleteEntity(Integer id) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        String entity = Entity.TERM_DEPOSIT_INTEREST_RATE.getValue();

        termDepositInterestRateRepository.findById(id)
                .ifPresentOrElse(_ -> termDepositInterestRateRepository.deleteById(id), () -> {
                    log.error(LogMessage.RESOURCE_NOT_FOUND, entity);
                    throw new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, entity));
                });

        log.info(LogMessage.RESOURCE_DELETE_SUCCESS, entity, id);
    }

    @Override
    public double getInterestRate(Currency currency, int depositMaturity, double balance) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        String entity = Entity.TERM_DEPOSIT_INTEREST_RATE.getValue();

        return termDepositInterestRateRepository.findByCurrencyAndDepositMaturityAndBalance(currency, depositMaturity, balance)
                .map(termDepositInterestRate -> {
                    log.info("{} exists for balance {}. Interval is between {} and {}", entity, balance, termDepositInterestRate.getMinimumBalance(), termDepositInterestRate.getMaximumBalance());
                    return termDepositInterestRate.getInterestRate();
                }).orElseThrow(() -> {
                    log.error("{} does not exist for balance {}", entity, balance);
                    return new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, entity));
                });
    }

    private TermDepositInterestRate findById(Integer id) {
        String entity =  Entity.TERM_DEPOSIT_INTEREST_RATE.getValue();
        TermDepositInterestRate termDepositInterestRate = termDepositInterestRateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, entity)));

        log.info(LogMessage.RESOURCE_FOUND, entity);

        return termDepositInterestRate;
    }

    private void checkUniqueness(TermDepositInterestRateDto request, TermDepositInterestRate previousTermDepositInterestRate) {
        /* The balance ranges of each term deposit interest rates in same deposit maturity should not overlap */
        Predicate<TermDepositInterestRate> termDepositInterestRatePredicate = termDepositInterestRate -> {
            boolean minimumBalanceCase = (request.getMinimumBalance() >= termDepositInterestRate.getMinimumBalance() && request.getMinimumBalance() <= termDepositInterestRate.getMaximumBalance());
            boolean maximumBalanceCase = (request.getMaximumBalance() >= termDepositInterestRate.getMinimumBalance() && request.getMaximumBalance() <= termDepositInterestRate.getMaximumBalance());
            boolean lessMinimumAndGreaterMaximumCase = (request.getMinimumBalance() <= termDepositInterestRate.getMinimumBalance() && request.getMaximumBalance() >= termDepositInterestRate.getMaximumBalance());
            return minimumBalanceCase || maximumBalanceCase || lessMinimumAndGreaterMaximumCase;
        };

        String entity = Entity.TERM_DEPOSIT_INTEREST_RATE.getValue();

        if (Optional.ofNullable(previousTermDepositInterestRate).isPresent()) {
            log.info("There is a previous {}", entity);

            boolean entityHasSameValues = request.getCurrency() == previousTermDepositInterestRate.getCurrency()
                    && Objects.equals(request.getMinimumBalance(), previousTermDepositInterestRate.getMinimumBalance())
                    && Objects.equals(request.getMaximumBalance(), previousTermDepositInterestRate.getMaximumBalance())
                    && Objects.equals(request.getDepositMaturity(), previousTermDepositInterestRate.getDepositMaturity());

            if (entityHasSameValues) {
                log.warn("Previous and updated fields (currency, minimum balance, maximum balance, deposit maturity) are same");
                return;
            }

            /* Same term deposit interest rates should not be compared */
            Predicate<TermDepositInterestRate> isPresentPredicate = termDepositInterestRate -> !Objects.equals(termDepositInterestRate.getId(), previousTermDepositInterestRate.getId());
            termDepositInterestRatePredicate = termDepositInterestRatePredicate.and(isPresentPredicate);
        } else {
            log.info("There is no previous {} for fields (currency, minimum balance, maximum balance, deposit maturity)", entity);
        }

        boolean inappropriateTermDepositInterestRateExists = termDepositInterestRateRepository.findAllByCurrencyAndDepositMaturity(request.getCurrency(), request.getDepositMaturity())
                .stream()
                .anyMatch(termDepositInterestRatePredicate);

        if (inappropriateTermDepositInterestRateExists) {
            throw new ResourceConflictException(entity + " balance range overlaps with other " + entity + "s' balance ranges");
        }

        log.info(LogMessage.RESOURCE_UNIQUE, entity);
    }
}
