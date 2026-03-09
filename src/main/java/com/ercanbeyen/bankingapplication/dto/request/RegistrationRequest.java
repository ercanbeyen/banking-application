package com.ercanbeyen.bankingapplication.dto.request;

import com.ercanbeyen.bankingapplication.dto.CustomerDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record RegistrationRequest(
        @Valid CustomerDto customerDto,
        @NotBlank(message = "Password should not be blank") String password,
        @NotNull(message = "Roles should not be null") Set<String> roles) {

}
