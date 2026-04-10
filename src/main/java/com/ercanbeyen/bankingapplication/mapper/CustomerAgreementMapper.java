package com.ercanbeyen.bankingapplication.mapper;

import com.ercanbeyen.bankingapplication.dto.CustomerAgreementDto;
import com.ercanbeyen.bankingapplication.entity.CustomerAgreement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CustomerAgreementMapper {
    @Mapping(source = "customer.nationalId", target = "customerNationalId")
    @Mapping(source = "agreement.title", target = "agreementTitle")
    CustomerAgreementDto entityToDto(CustomerAgreement customerAgreement);
}
