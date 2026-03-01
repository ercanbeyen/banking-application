package com.ercanbeyen.bankingapplication.dto.request;

import com.ercanbeyen.bankingapplication.dto.CustomerDto;

import java.util.Set;

public record RegistrationRequest(String username, String password, Set<String> roles, CustomerDto customerDto) {

}
