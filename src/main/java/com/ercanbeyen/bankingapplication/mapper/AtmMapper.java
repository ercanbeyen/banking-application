package com.ercanbeyen.bankingapplication.mapper;

import com.ercanbeyen.bankingapplication.dto.AtmDto;
import com.ercanbeyen.bankingapplication.entity.Atm;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AtmMapper {
    AtmDto entityToDto(Atm atm);
    Atm dtoToEntity(AtmDto atmDto);
}
