package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.AddressActivity;
import com.ercanbeyen.bankingapplication.constant.enums.AddressType;
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
import com.ercanbeyen.bankingapplication.service.AddressService;
import com.ercanbeyen.bankingapplication.util.LoggingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

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
        Address address = findById(id);
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
        log.info(LogMessages.RESOURCE_CREATE_SUCCESS, Entity.ADDRESS.getValue(), savedAddress.getId());

        return addressMapper.entityToDto(savedAddress);
    }

    @Override
    public AddressDto updateEntity(String id, AddressDto request) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        Address address = findById(id);
        checkRequestBeforeUpdateEntity(request, address.getType());

        address.setCity(request.city());
        address.setDetails(request.details());
        address.setOwnership(request.ownership());
        address.setPhoneNumber(request.phoneNumber());
        address.setZipCode(request.zipCode());

        return addressMapper.entityToDto(addressRepository.save(address));
    }

    @Override
    public String modifyRelationshipBetweenAddressAndCustomer(String addressId, String customerNationalId, AddressActivity activity) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        Address address = findById(addressId);
        Customer customer = customerService.findByNationalId(customerNationalId);

        BiPredicate<Address, Customer> isCustomerRelatedWithAddress = (givenAddress, givenCustomer) -> givenAddress.getCustomers().contains(givenCustomer);

        String customerEntity = Entity.CUSTOMER.getValue();
        String addressEntity = Entity.ADDRESS.getValue();
        String response;

        if (activity == AddressActivity.ADD) {
            if (isCustomerRelatedWithAddress.test(address, customer)) {
                throw new ResourceConflictException(String.format("%s is already related with %s", customerEntity, addressEntity));
            }

            address.getCustomers().add(customer);
            addressRepository.save(address);

            response = String.format("%s is successfully related with %s", customerEntity, addressEntity);
        } else {
            if (!isCustomerRelatedWithAddress.test(address, customer)) {
                throw new ResourceConflictException(String.format("%s is not already related with %s", customerEntity, addressEntity));
            }

            address.getCustomers().remove(customer);

            if (address.getCustomers().isEmpty()) {
                log.info("No more related customers exist, so address is going to be deleted");
                addressRepository.delete(address);
            } else {
                log.info("Still related customers exist, so just update the database");
                addressRepository.save(address);
            }

            response = String.format("Relation of %s is successfully terminated with %s", customerEntity, addressEntity);
        }

        return response;
    }

    private Address findById(String id) {
        String entity = Entity.ADDRESS.getValue();
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessages.NOT_FOUND, entity)));

        log.info(LogMessages.RESOURCE_FOUND, entity);

        return address;
    }

    private static void checkRequestBeforeUpdateEntity(AddressDto request, AddressType addressType) {
        if (request.type() != addressType) {
            throw new ResourceConflictException("Address type should not be changed");
        }
    }
}
