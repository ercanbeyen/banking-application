package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.Currency;
import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.LogMessage;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.dto.FeeDto;
import com.ercanbeyen.bankingapplication.entity.Fee;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.mapper.FeeMapper;
import com.ercanbeyen.bankingapplication.option.FeeFilteringOption;
import com.ercanbeyen.bankingapplication.repository.FeeRepository;
import com.ercanbeyen.bankingapplication.service.FeeService;
import com.ercanbeyen.bankingapplication.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

@Slf4j
@RequiredArgsConstructor
@Service
public class FeeServiceImpl implements FeeService {
    private final FeeRepository feeRepository;
    private final FeeMapper feeMapper;

    @Override
    public List<FeeDto> getEntities(FeeFilteringOption filteringOption) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Predicate<Fee> feePredicate = fee -> {
            boolean currencyFilter = (Optional.ofNullable(filteringOption.getCurrency()).isEmpty() || fee.getCurrency() == filteringOption.getCurrency());
            boolean depositPeriodFilter = (Optional.ofNullable(filteringOption.getDepositPeriod()).isEmpty() || fee.getDepositPeriod() == filteringOption.getDepositPeriod().intValue());
            boolean updatedAtFilter = (Optional.ofNullable(filteringOption.getUpdatedAt()).isEmpty() || fee.getUpdatedAt().toLocalDate().isEqual(filteringOption.getUpdatedAt()));
            return currencyFilter && depositPeriodFilter && updatedAtFilter;
        };

        Comparator<Fee> feeComparator = Comparator.comparing(Fee::getUpdatedAt).reversed();

        return feeRepository.findAll()
                .stream()
                .filter(feePredicate)
                .sorted(feeComparator)
                .map(feeMapper::entityToDto)
                .toList();
    }

    @Override
    public FeeDto getEntity(Integer id) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());
        return feeMapper.entityToDto(findById(id));
    }

    @Override
    public FeeDto createEntity(FeeDto request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        checkUniqueness(request, null);
        Fee fee = feeMapper.dtoToEntity(request);

        Fee savedFee = feeRepository.save(fee);
        log.info(LogMessage.RESOURCE_CREATE_SUCCESS, Entity.FEE.getValue(), savedFee.getId());

        return feeMapper.entityToDto(savedFee);
    }

    @Override
    public FeeDto updateEntity(Integer id, FeeDto request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Fee fee = findById(id);
        checkUniqueness(request, fee);

        fee.setCurrency(request.getCurrency());
        fee.setMinimumAmount(request.getMinimumAmount());
        fee.setMaximumAmount(request.getMaximumAmount());
        fee.setDepositPeriod(request.getDepositPeriod());
        fee.setInterestRatio(request.getInterestRatio());

        return feeMapper.entityToDto(feeRepository.save(fee));
    }

    @Override
    public void deleteEntity(Integer id) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        String entity = Entity.FEE.getValue();

        feeRepository.findById(id)
                .ifPresentOrElse(fee -> feeRepository.deleteById(id), () -> {
                    log.error(LogMessage.RESOURCE_NOT_FOUND, entity);
                    throw new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, entity));
                });

        log.info(LogMessage.RESOURCE_DELETE_SUCCESS, entity, id);
    }

    @Override
    public double getInterestRatio(Currency currency, int depositPeriod, double balance) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        return feeRepository.findByCurrencyAndDepositPeriodAndBalance(currency, depositPeriod, balance)
                .map(fee -> {
                    log.info("Fee exists for balance {}. Interval is between {} and {}", balance, fee.getMinimumAmount(), fee.getMaximumAmount());
                    return fee.getInterestRatio();
                }).orElseThrow(() -> {
                    log.error("Fee does not exist for balance {}", balance);
                    return new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, Entity.FEE.getValue()));
                });
    }

    private Fee findById(Integer id) {
        String entity =  Entity.FEE.getValue();
        Fee fee = feeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, entity)));

        log.info(LogMessage.RESOURCE_FOUND, entity);

        return fee;
    }

    private void checkUniqueness(FeeDto request, Fee previousFee) {
        /* Fee's minimum & maximum interval should not intersect with other fees' intervals */
        Predicate<Fee> feePredicate = fee -> {
            boolean minimumAmountCase = (request.getMinimumAmount() >= fee.getMinimumAmount() && request.getMinimumAmount() <= fee.getMaximumAmount());
            boolean maximumAmountCase = (request.getMaximumAmount() >= fee.getMinimumAmount() && request.getMaximumAmount() <= fee.getMaximumAmount());
            boolean lessMinimumAndGreaterMaximumCase = (request.getMinimumAmount() <= fee.getMinimumAmount() && request.getMaximumAmount() >= fee.getMaximumAmount());
            return minimumAmountCase || maximumAmountCase || lessMinimumAndGreaterMaximumCase;
        };

        if (Optional.ofNullable(previousFee).isPresent()) {
            log.info("There is a previous fee");

            boolean entityHasSameValues = request.getCurrency() == previousFee.getCurrency()
                    && Objects.equals(request.getMinimumAmount(), previousFee.getMinimumAmount())
                    && Objects.equals(request.getMaximumAmount(), previousFee.getMaximumAmount())
                    && Objects.equals(request.getDepositPeriod(), previousFee.getDepositPeriod());

            if (entityHasSameValues) {
                log.warn("Previous and updated fields (currency, minimum amount, maximum amount, deposit period) are same");
                return;
            }

            /* Same fees should not be compared */
            Predicate<Fee> isPresentPredicate = fee -> !Objects.equals(fee.getId(), previousFee.getId());
            feePredicate = feePredicate.and(isPresentPredicate);
        } else {
            log.info("There is no previous fee for fields (currency, minimum amount, maximum amount, deposit period)");
        }

        boolean inappropriateFeeExists = feeRepository.findAllByCurrencyAndDepositPeriod(request.getCurrency(), request.getDepositPeriod())
                .stream()
                .anyMatch(feePredicate);

        if (inappropriateFeeExists) {
            throw new ResourceConflictException("Fee's minimum & maximum interval intersects with other fees' intervals");
        }

        log.info(LogMessage.RESOURCE_UNIQUE, Entity.FEE.getValue());
    }
}
