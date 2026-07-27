package com.ercanbeyen.bankingapplication.mapper;

import com.ercanbeyen.bankingapplication.dto.DailyAccountActivityLimitDto;
import com.ercanbeyen.bankingapplication.entity.DailyAccountActivityLimit;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DailyAccountActivityLimitMapper {
    DailyAccountActivityLimitDto entityToDto(DailyAccountActivityLimit dailyAccountActivityLimit);
    DailyAccountActivityLimit dtoToEntity(DailyAccountActivityLimitDto dailyAccountActivityLimitDto);
}
