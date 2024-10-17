package com.ercanbeyen.bankingapplication.mapper;

import com.ercanbeyen.bankingapplication.dto.ChargeDto;
import com.ercanbeyen.bankingapplication.entity.Charge;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ChargeMapper {
    ChargeDto entityToDto(Charge charge);
    Charge dtoToEntity(ChargeDto chargeDto);
}
