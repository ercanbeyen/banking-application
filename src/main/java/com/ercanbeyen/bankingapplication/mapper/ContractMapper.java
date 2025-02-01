package com.ercanbeyen.bankingapplication.mapper;

import com.ercanbeyen.bankingapplication.dto.ContractDto;
import com.ercanbeyen.bankingapplication.entity.Contract;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ContractMapper {
    @Mapping(target = "fileId", source = "file.id")
    ContractDto entityToDto(Contract contract);
    Contract dtoToEntity(ContractDto contractDto);
}
