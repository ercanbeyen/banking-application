package com.ercanbeyen.bankingapplication.mapper;

import com.ercanbeyen.bankingapplication.dto.BranchOrderDto;
import com.ercanbeyen.bankingapplication.entity.AccountActivity;
import com.ercanbeyen.bankingapplication.entity.BranchOrder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Optional;

@Mapper(componentModel = "spring")
public interface BranchOrderMapper {
    @Mapping(target = "customerNationalId", source = "customer.nationalId")
    @Mapping(target = "branchName", source = "branch.name")
    @Mapping(target = "accountActivityId", source = "accountActivity", qualifiedByName = "entityToId")
    BranchOrderDto entityToDto(BranchOrder branchOrder);
    BranchOrder dtoToEntity(BranchOrderDto branchOrderDto);

    @Named("entityToId")
    static String entityToId(AccountActivity accountActivity) {
        return Optional.ofNullable(accountActivity).isPresent() ? accountActivity.getId() : null;
    }
}
