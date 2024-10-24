package com.ercanbeyen.bankingapplication.mapper;

import com.ercanbeyen.bankingapplication.dto.DailyActivityLimitDto;
import com.ercanbeyen.bankingapplication.entity.DailyActivityLimit;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DailyActivityLimitMapper {
    DailyActivityLimitDto entityToDto(DailyActivityLimit dailyActivityLimit);
    DailyActivityLimit dtoToEntity(DailyActivityLimitDto dailyActivityLimitDto);
}
