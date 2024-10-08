package com.ercanbeyen.bankingapplication.mapper;

import com.ercanbeyen.bankingapplication.dto.ExchangeDto;
import com.ercanbeyen.bankingapplication.entity.Exchange;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ExchangeMapper {
    ExchangeDto entityToDto(Exchange exchange);
    Exchange dtoToEntity(ExchangeDto exchangeDto);
}
