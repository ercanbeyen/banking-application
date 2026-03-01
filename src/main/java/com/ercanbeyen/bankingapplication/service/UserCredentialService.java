package com.ercanbeyen.bankingapplication.service;

import com.ercanbeyen.bankingapplication.dto.request.RegistrationRequest;

public interface UserCredentialService {
    void createUserCredential(RegistrationRequest request);
    boolean existsByUsername(String username);
}
