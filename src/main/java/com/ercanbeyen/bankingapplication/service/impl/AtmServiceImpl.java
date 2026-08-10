package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.AddressType;
import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.LogMessage;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.dto.AtmDto;
import com.ercanbeyen.bankingapplication.dto.option.AtmFilteringOption;
import com.ercanbeyen.bankingapplication.embeddable.Address;
import com.ercanbeyen.bankingapplication.entity.Atm;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.mapper.AtmMapper;
import com.ercanbeyen.bankingapplication.repository.AtmRepository;
import com.ercanbeyen.bankingapplication.service.AtmService;
import com.ercanbeyen.bankingapplication.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Slf4j
@Service
@RequiredArgsConstructor
public class AtmServiceImpl implements AtmService {
    private final AtmRepository atmRepository;
    private final AtmMapper atmMapper;

    @Override
    public List<AtmDto> getEntities(AtmFilteringOption filteringOption) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Predicate<Atm> atmPredicate = atm -> {
            String cityOption = filteringOption.getCity();
            String districtOption = filteringOption.getDistrict();
            LocalDate createdAtOption = filteringOption.getCreatedAt();
            Address address = atm.getAddress();

            boolean cityFilter = Optional.ofNullable(cityOption).isEmpty() || address.getCity().equals(cityOption);
            boolean districtFilter = Optional.ofNullable(districtOption).isEmpty() || address.getDistrict().equals(districtOption);
            boolean createdAtFilter = Optional.ofNullable(createdAtOption).isEmpty() || LocalDate.ofInstant(atm.getCreatedAt(), ZoneId.systemDefault()).isEqual(createdAtOption);

            return cityFilter && districtFilter && createdAtFilter;
        };

        return atmRepository.findAll()
                .stream()
                .filter(atmPredicate)
                .map(atmMapper::entityToDto)
                .toList();
    }

    @Override
    public AtmDto getEntity(Integer id) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());
        return atmMapper.entityToDto(findById(id));
    }

    @Override
    public AtmDto createEntity(AtmDto request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Atm atm = atmMapper.dtoToEntity(request);
        atm.getAddress().setType(AddressType.WORK);

        Atm savedAtm = atmRepository.save(atm);
        log.info(LogMessage.RESOURCE_CREATE_SUCCESS, Entity.ATM.getValue(), savedAtm.getId());

        return atmMapper.entityToDto(savedAtm);
    }

    @Override
    public AtmDto updateEntity(Integer id, AtmDto request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Atm atm = findById(id);

        atm.setAddress(request.getAddress());
        atm.getAddress().setType(AddressType.WORK);

        return atmMapper.entityToDto(atmRepository.save(atm));
    }

    @Override
    public void deleteEntity(Integer id) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        atmRepository.findById(id).ifPresentOrElse(
                atm -> {
                    atmRepository.delete(atm);
                    log.info(LogMessage.RESOURCE_DELETE_SUCCESS, Entity.ATM.getValue(), id);
                },
                () -> {
                    log.error(LogMessage.RESOURCE_NOT_FOUND, Entity.ATM.getValue());
                    throw new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, Entity.ATM.getValue()));
                });
    }

    private Atm findById(Integer id) {
        return atmRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, Entity.ATM.getValue())));
    }
}
