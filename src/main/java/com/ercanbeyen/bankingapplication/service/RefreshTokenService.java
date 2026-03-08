package com.ercanbeyen.bankingapplication.service;

import com.ercanbeyen.bankingapplication.model.RefreshToken;

public interface RefreshTokenService {
    void createRefreshToken(String token);
    RefreshToken findByToken(String token);
    void verifyExpiration(String token);
    void revokeAllRefreshTokens(String username);
}
