package com.ercanbeyen.bankingapplication.service;

import com.ercanbeyen.bankingapplication.dto.UserCredentialsDto;
import com.ercanbeyen.bankingapplication.dto.request.UpdatePasswordRequest;
import com.ercanbeyen.bankingapplication.entity.UserCredentials;

import java.util.Set;

public interface UserCredentialsService {
    void createUserCredentials(UserCredentialsDto request);
    void loginSucceeded(String username);
    void loginFailed(String username);
    void checkLockStatus(UserCredentials userCredentials);
    void updateRoles(String username, Set<String> roles);
    void updatePassword(String username, UpdatePasswordRequest request);
    UserCredentials findByUsername(String username);
    boolean existsByUsername(String username);
}
