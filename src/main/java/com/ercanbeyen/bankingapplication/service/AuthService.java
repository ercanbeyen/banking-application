package com.ercanbeyen.bankingapplication.service;

import com.ercanbeyen.bankingapplication.dto.IncorrectLoginAttemptDto;
import com.ercanbeyen.bankingapplication.dto.request.LoginRequest;
import com.ercanbeyen.bankingapplication.dto.request.RegistrationRequest;
import com.ercanbeyen.bankingapplication.dto.request.UpdatePasswordRequest;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface AuthService {
    void loginUser(LoginRequest loginRequest);
    Map<String, String> generateTokens(String username);
    void registerUser(RegistrationRequest request);
    Map<String, String> refreshToken(String token);
    Set<String> getRoles(String username);
    void updateRoles(String username, Set<String> roles);
    void updatePassword(String username, UpdatePasswordRequest request);
    List<IncorrectLoginAttemptDto> getIncorrectLoginAttempts(String username);
}
