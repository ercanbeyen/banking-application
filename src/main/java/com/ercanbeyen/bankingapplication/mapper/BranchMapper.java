package com.ercanbeyen.bankingapplication.mapper;

import com.ercanbeyen.bankingapplication.dto.BranchDto;
import com.ercanbeyen.bankingapplication.entity.Account;
import com.ercanbeyen.bankingapplication.entity.Branch;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface BranchMapper {
    @Mapping(source = "accounts", target = "accountIds", qualifiedByName = "entityToId")
    BranchDto entityToDto(Branch branch);
    Branch dtoToEntity(BranchDto branchDto);

    @Named("entityToId")
    static int entityToId(Account account) {
        return account.getId();
    }
}
