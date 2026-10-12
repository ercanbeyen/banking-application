package com.ercanbeyen.bankingapplication.mapper;

import com.ercanbeyen.bankingapplication.dto.BranchOrderDto;
import com.ercanbeyen.bankingapplication.entity.AccountActivity;
import com.ercanbeyen.bankingapplication.entity.BranchOrder;
import com.ercanbeyen.bankingapplication.entity.File;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Optional;

@Mapper(componentModel = "spring")
public interface BranchOrderMapper {
    @Mapping(target = "customerNationalId", source = "customer.nationalId")
    @Mapping(target = "branchName", source = "branch.name")
    @Mapping(target = "accountActivityId", source = "accountActivity", qualifiedByName = "accountActivityToId")
    @Mapping(target = "fileId", source = "petition", qualifiedByName = "petitionToFileId")
    BranchOrderDto entityToDto(BranchOrder branchOrder);
    BranchOrder dtoToEntity(BranchOrderDto branchOrderDto);

    @Named("accountActivityToId")
    static String accountActivityToId(AccountActivity accountActivity) {
        return Optional.ofNullable(accountActivity).isPresent() ? accountActivity.getId() : null;
    }

    @Named("petitionToFileId")
    static String petitionToFileId(File petition) {
        return Optional.ofNullable(petition).isPresent() ? petition.getId() : null;
    }
}
