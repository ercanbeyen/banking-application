package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.LogMessages;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessages;
import com.ercanbeyen.bankingapplication.dto.FeeDto;
import com.ercanbeyen.bankingapplication.entity.Fee;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.mapper.FeeMapper;
import com.ercanbeyen.bankingapplication.option.FeeFilteringOptions;
import com.ercanbeyen.bankingapplication.repository.FeeRepository;
import com.ercanbeyen.bankingapplication.service.BaseService;
import com.ercanbeyen.bankingapplication.util.LoggingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class FeeService implements BaseService<FeeDto, FeeFilteringOptions> {
    private final FeeRepository feeRepository;
    private final FeeMapper feeMapper;

    @Override
    public List<FeeDto> getEntities(FeeFilteringOptions options) {
        return feeRepository.findAll()
                .stream()
                .map(feeMapper::entityToDto)
                .toList();
    }

    @Override
    public FeeDto getEntity(Integer id) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());
        return feeMapper.entityToDto(findById(id));
    }

    @Override
    public FeeDto createEntity(FeeDto request) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        Fee fee = feeMapper.dtoToEntity(request);

        Fee savedFee = feeRepository.save(fee);
        log.info(LogMessages.RESOURCE_CREATE_SUCCESS, Entity.FEE.getValue(), savedFee.getId());

        return feeMapper.entityToDto(savedFee);
    }

    @Override
    public FeeDto updateEntity(Integer id, FeeDto request) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        Fee fee = findById(id);

        fee.setCurrency(request.getCurrency());
        fee.setMinimumAmount(request.getMinimumAmount());
        fee.setMaximumAmount(request.getMinimumAmount());
        fee.setDepositPeriod(request.getDepositPeriod());
        fee.setInterestRatio(request.getInterestRatio());

        return feeMapper.entityToDto(feeRepository.save(fee));
    }

    @Override
    public void deleteEntity(Integer id) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        String entity = Entity.FEE.getValue();

        feeRepository.findById(id)
                .ifPresentOrElse(fee -> feeRepository.deleteById(id), () -> {
                    log.error(LogMessages.RESOURCE_NOT_FOUND, entity);
                    throw new ResourceNotFoundException(String.format(ResponseMessages.NOT_FOUND, entity));
                });

        log.info(LogMessages.RESOURCE_DELETE_SUCCESS, entity, id);
    }

    private Fee findById(Integer id) {
        String entity =  Entity.FEE.getValue();
        Fee fee = feeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessages.NOT_FOUND, entity)));

        log.info(LogMessages.RESOURCE_FOUND, entity);

        return fee;
    }
}
