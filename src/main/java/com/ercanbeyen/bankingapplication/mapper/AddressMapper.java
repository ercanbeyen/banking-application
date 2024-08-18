package com.ercanbeyen.bankingapplication.mapper;

import com.ercanbeyen.bankingapplication.dto.AddressDto;
import com.ercanbeyen.bankingapplication.entity.Address;
import com.ercanbeyen.bankingapplication.entity.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface AddressMapper {
    @Mapping(source = "customers", target = "customerNationalIds", qualifiedByName = "entityToId")
    AddressDto entityToDto(Address address);
    Address dtoToEntity(AddressDto addressDto);

    @Named("entityToId")
    static String entityToId(Customer customer) {
        return customer.getNationalId();
    }
}

