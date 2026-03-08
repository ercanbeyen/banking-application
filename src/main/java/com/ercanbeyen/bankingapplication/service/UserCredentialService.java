package com.ercanbeyen.bankingapplication.service;

import com.ercanbeyen.bankingapplication.dto.request.RegistrationRequest;
import com.ercanbeyen.bankingapplication.model.UserCredential;

public interface UserCredentialService {
    void createUserCredential(RegistrationRequest request);
    UserCredential findByUsername(String username);
    boolean existsByUsername(String username);
}
