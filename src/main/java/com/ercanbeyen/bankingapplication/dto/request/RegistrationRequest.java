package com.ercanbeyen.bankingapplication.dto.request;

import com.ercanbeyen.bankingapplication.annotation.PasswordRequest;
import com.ercanbeyen.bankingapplication.annotation.RolesRequest;
import com.ercanbeyen.bankingapplication.dto.CustomerDto;
import jakarta.validation.Valid;

import java.util.Set;

public record RegistrationRequest(
        @Valid CustomerDto customerDto,
        @PasswordRequest String password,
        @RolesRequest Set<String> roles) {

}
