package com.ercanbeyen.bankingapplication.service;

import com.ercanbeyen.bankingapplication.constant.enums.ERole;
import com.ercanbeyen.bankingapplication.dto.IncorrectLoginAttemptDto;
import com.ercanbeyen.bankingapplication.dto.request.LoginRequest;
import com.ercanbeyen.bankingapplication.dto.request.RegistrationRequest;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface AuthService {
    Map<String, String> loginUser(LoginRequest loginRequest);
    void registerUser(RegistrationRequest request);
    Map<String, String> refreshToken(String token);
    Set<ERole> getRoles(String username);
    void updateRoles(String username, Set<String> roles);
    void updatePassword(String username, String password);
    List<IncorrectLoginAttemptDto> getIncorrectLoginAttempts(String username);
}
