package com.ercanbeyen.bankingapplication.mapper;

import com.ercanbeyen.bankingapplication.dto.ExchangeDto;
import com.ercanbeyen.bankingapplication.model.Exchange;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ExchangeMapper {
    ExchangeDto entityToDto(Exchange exchange);
    Exchange dtoToEntity(ExchangeDto exchangeDto);
}
