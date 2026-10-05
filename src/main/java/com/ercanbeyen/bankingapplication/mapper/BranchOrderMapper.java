package com.ercanbeyen.bankingapplication.mapper;

import com.ercanbeyen.bankingapplication.dto.BranchOrderDto;
import com.ercanbeyen.bankingapplication.entity.BranchOrder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BranchOrderMapper {
    @Mapping(target = "customerNationalId", source = "customer.nationalId")
    @Mapping(target = "branchName", source = "branch.name")
    BranchOrderDto entityToDto(BranchOrder branchOrder);
    BranchOrder dtoToEntity(BranchOrderDto branchOrderDto);
}
