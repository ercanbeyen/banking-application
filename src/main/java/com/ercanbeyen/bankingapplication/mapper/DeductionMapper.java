package com.ercanbeyen.bankingapplication.mapper;

import com.ercanbeyen.bankingapplication.dto.DeductionDto;
import com.ercanbeyen.bankingapplication.entity.Deduction;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DeductionMapper {
    DeductionDto entityToDto(Deduction deduction);
    Deduction dtoToEntity(DeductionDto deductionDto);
}
