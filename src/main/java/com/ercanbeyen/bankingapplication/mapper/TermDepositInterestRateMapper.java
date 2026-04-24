package com.ercanbeyen.bankingapplication.mapper;

import com.ercanbeyen.bankingapplication.dto.TermDepositInterestRateDto;
import com.ercanbeyen.bankingapplication.entity.TermDepositInterestRate;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TermDepositInterestRateMapper {
    TermDepositInterestRateDto entityToDto(TermDepositInterestRate termDepositInterestRate);
    TermDepositInterestRate dtoToEntity(TermDepositInterestRateDto termDepositInterestRateDto);
}
