package com.ercanbeyen.bankingapplication.mapper;

import com.ercanbeyen.bankingapplication.dto.FeeDto;
import com.ercanbeyen.bankingapplication.entity.Fee;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FeeMapper {
    FeeDto entityToDto(Fee fee);
    Fee dtoToEntity(FeeDto feeDto);
}
