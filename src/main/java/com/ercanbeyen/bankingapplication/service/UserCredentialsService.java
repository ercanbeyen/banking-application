package com.ercanbeyen.bankingapplication.service;

import com.ercanbeyen.bankingapplication.constant.enums.ERole;
import com.ercanbeyen.bankingapplication.dto.UserCredentialsDto;
import com.ercanbeyen.bankingapplication.entity.UserCredentials;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Set;

public interface UserCredentialsService {
    void createUserCredentials(UserCredentialsDto request);
    void loginSucceeded(String username);
    void loginFailed(String username, HttpServletRequest httpServletRequest);
    void checkLockStatus(UserCredentials userCredentials);
    UserCredentials findByUsername(String username);
    Set<ERole> getRoles(String username);
    void updateRoles(String username, Set<String> roles);
    void updatePassword(String username, String password);
    boolean existsByUsername(String username);
}
