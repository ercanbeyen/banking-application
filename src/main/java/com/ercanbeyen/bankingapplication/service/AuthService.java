package com.ercanbeyen.bankingapplication.service;

import com.ercanbeyen.bankingapplication.dto.request.LoginRequest;
import com.ercanbeyen.bankingapplication.dto.request.RegistrationRequest;

import java.util.Map;

public interface AuthService {
    Map<String, String> loginUser(LoginRequest request);
    void registerUser(RegistrationRequest request);
    Map<String, String> refreshToken(String token);
}
