package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.LogMessages;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessages;
import com.ercanbeyen.bankingapplication.dto.AddressDto;
import com.ercanbeyen.bankingapplication.dto.request.CreateAddressRequest;
import com.ercanbeyen.bankingapplication.entity.Address;
import com.ercanbeyen.bankingapplication.entity.Customer;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.mapper.AddressMapper;
import com.ercanbeyen.bankingapplication.repository.AddressRepository;
import com.ercanbeyen.bankingapplication.repository.CustomerRepository;
import com.ercanbeyen.bankingapplication.service.AddressService;
import com.ercanbeyen.bankingapplication.util.LoggingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AddressServiceImpl implements AddressService {
    private final AddressRepository addressRepository;
    private final CustomerService customerService;
    private final AddressMapper addressMapper;

    @Override
    public List<AddressDto> getEntities() {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        List<AddressDto> addressDtos = new ArrayList<>();

        addressRepository.findAll()
                .forEach(address -> addressDtos.add(addressMapper.entityToDto(address)));

        return addressDtos;
    }

    @Override
    public AddressDto getEntity(String id) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessages.NOT_FOUND, Entity.ADDRESS.getValue())));

        log.info(LogMessages.RESOURCE_FOUND, Entity.ADDRESS.getValue());

        return addressMapper.entityToDto(address);
    }

    @Override
    public AddressDto createEntity(CreateAddressRequest request) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        Customer customer = customerService.findByNationalId(request.customerNationalId());

        Address address = new Address();
        address.getCustomers().add(customer);
        address.setType(request.type());
        address.setCity(request.city());
        address.setZipCode(request.zipCode());
        address.setPhoneNumber(request.phoneNumber());
        address.setDetails(request.details());
        address.setOwnership(request.ownership());

        Address savedAddress = addressRepository.save(address);
        log.info(LogMessages.RESOURCE_CREATE_SUCCESS, Entity.ADDRESS.getValue());

        return addressMapper.entityToDto(savedAddress);
    }

    @Override
    public AddressDto updateEntity(String id, AddressDto request) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessages.NOT_FOUND, Entity.ADDRESS.getValue())));

        log.info(LogMessages.RESOURCE_FOUND, Entity.ADDRESS.getValue());

        Set<Customer> customers = new HashSet<>();

        request.customerNationalIds().forEach(nationalId -> customers.add(customerService.findByNationalId(nationalId)));

        address.setCustomers(customers);
        address.setCity(request.city());
        address.setType(request.type());
        address.setDetails(request.details());
        address.setOwnership(request.ownership());
        address.setPhoneNumber(request.phoneNumber());
        address.setZipCode(request.zipCode());

        return addressMapper.entityToDto(addressRepository.save(address));
    }

    @Override
    public void deleteEntity(String id) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        String entity = Entity.ADDRESS.getValue();

        addressRepository.findById(id)
                .ifPresentOrElse(
                        address -> addressRepository.deleteById(id),
                        () -> {
                            log.error(LogMessages.RESOURCE_NOT_FOUND, entity);
                            throw new ResourceNotFoundException(String.format(ResponseMessages.NOT_FOUND, entity));
                        });

        log.info(LogMessages.RESOURCE_DELETE_SUCCESS, Entity.NOTIFICATION.getValue(), id);
    }
}
