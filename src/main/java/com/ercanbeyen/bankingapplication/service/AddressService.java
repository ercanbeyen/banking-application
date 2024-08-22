package com.ercanbeyen.bankingapplication.service;

import com.ercanbeyen.bankingapplication.constant.enums.AddressActivity;
import com.ercanbeyen.bankingapplication.dto.AddressDto;
import com.ercanbeyen.bankingapplication.dto.request.CreateAddressRequest;

import java.util.List;

public interface AddressService {
    List<AddressDto> getEntities();
    AddressDto getEntity(String id);
    AddressDto createEntity(CreateAddressRequest request);
    AddressDto updateEntity(String id, AddressDto request);
    void deleteEntity(String id);
    String modifyRelationshipBetweenAddressAndCustomer(String addressId, String customerNationalId, AddressActivity activity);
}
