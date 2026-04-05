package com.ercanbeyen.bankingapplication.service;

import com.ercanbeyen.bankingapplication.constant.enums.ERole;
import com.ercanbeyen.bankingapplication.dto.request.RegistrationRequest;
import com.ercanbeyen.bankingapplication.model.UserCredential;

import java.util.Set;

public interface UserCredentialService {
    void createUserCredential(RegistrationRequest request);
    UserCredential findByUsername(String username);
    Set<ERole> getRoles(String username);
    void updateRoles(String username, Set<String> roles);
    void updatePassword(String username, String password);
    boolean existsByUsername(String username);
}
