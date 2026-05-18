package com.ercanbeyen.bankingapplication.mapper;

import com.ercanbeyen.bankingapplication.dto.IncorrectLoginAttemptDto;
import com.ercanbeyen.bankingapplication.entity.IncorrectLoginAttempt;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface IncorrectLoginAttemptMapper {
    @Mapping(source = "userCredentials.username", target = "username")
    IncorrectLoginAttemptDto entityToDto(IncorrectLoginAttempt incorrectLoginAttempt);
    IncorrectLoginAttempt dtoToEntity(IncorrectLoginAttemptDto incorrectLoginAttemptDto);
}
