package com.ercanbeyen.bankingapplication.dto.response;

public record LoginResponse(
        boolean is2FARequired,
        String email,
        String accessToken,
        String refreshToken,
        String message) {
}
