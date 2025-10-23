package com.ercanbeyen.bankingapplication.mapper;

import com.ercanbeyen.bankingapplication.dto.AccountActivityDto;
import com.ercanbeyen.bankingapplication.entity.AccountActivity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccountActivityMapper {
    @Mapping(target = "senderAccountId", source = "senderAccount.id")
    @Mapping(target = "recipientAccountId", source = "recipientAccount.id")
    AccountActivityDto entityToDto(AccountActivity accountActivity);
}
