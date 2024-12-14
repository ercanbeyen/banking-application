package com.ercanbeyen.bankingapplication.mapper;

import com.ercanbeyen.bankingapplication.dto.CashFlowCalendarDto;
import com.ercanbeyen.bankingapplication.entity.CashFlowCalendar;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CashFlowCalendarMapper {
    @Mapping(target = "customerNationalId", source = "customer.nationalId")
    CashFlowCalendarDto entityToDto(CashFlowCalendar cashFlowCalendar);
}
