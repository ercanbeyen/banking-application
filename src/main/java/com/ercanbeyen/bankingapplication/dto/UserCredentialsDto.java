package com.ercanbeyen.bankingapplication.dto;

import java.util.Set;

public record UserCredentialsDto(String username, Integer customerId, String password, Set<String> roles) {

}
