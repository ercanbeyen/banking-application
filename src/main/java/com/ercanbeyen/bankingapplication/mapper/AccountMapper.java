package com.ercanbeyen.bankingapplication.mapper;

import com.ercanbeyen.bankingapplication.dto.AccountDto;
import com.ercanbeyen.bankingapplication.entity.Account;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccountMapper {
    @Mapping(target = "customerNationalId", source = "customer.nationalId")
    @Mapping(target = "branchName", source = "branch.name")
    @Mapping(target = "isBlocked", source = "blocked")
    AccountDto entityToDto(Account account);
    Account dtoToEntity(AccountDto accountDto);
}
