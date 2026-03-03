package com.ercanbeyen.bankingapplication.security.config;

import com.ercanbeyen.bankingapplication.security.service.JwtService;
import com.ercanbeyen.bankingapplication.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomLogoutHandler implements LogoutHandler {
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        String token = jwtService.extractTokenFromHeader(request);
        refreshTokenService.verifyExpiration(token);

        String username = jwtService.extractUsername(token);
        refreshTokenService.revokeAllRefreshTokens(username);

        SecurityContextHolder.clearContext();
    }
}
