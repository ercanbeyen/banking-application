package com.ercanbeyen.bankingapplication.mapper;

import com.ercanbeyen.bankingapplication.dto.IncorrectLoginAttemptDto;
import com.ercanbeyen.bankingapplication.entity.IncorrectLoginAttempt;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface IncorrectLoginAttemptMapper {
    IncorrectLoginAttemptDto entityToDto(IncorrectLoginAttempt incorrectLoginAttempt);
    IncorrectLoginAttempt dtoToEntity(IncorrectLoginAttemptDto incorrectLoginAttemptDto);
}
