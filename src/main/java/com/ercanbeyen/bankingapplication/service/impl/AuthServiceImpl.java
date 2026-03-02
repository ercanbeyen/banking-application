package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.message.LogMessage;
import com.ercanbeyen.bankingapplication.dto.request.LoginRequest;
import com.ercanbeyen.bankingapplication.dto.request.RegistrationRequest;
import com.ercanbeyen.bankingapplication.exception.BadRequestException;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import com.ercanbeyen.bankingapplication.security.service.JwtService;
import com.ercanbeyen.bankingapplication.security.service.UserDetailsImpl;
import com.ercanbeyen.bankingapplication.service.AuthService;
import com.ercanbeyen.bankingapplication.service.CustomerService;
import com.ercanbeyen.bankingapplication.service.UserCredentialService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final AuthenticationManager authenticationManager;
    private final CustomerService customerService;
    private final UserCredentialService userCredentialService;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;

    @Override
    public Map<String, String> loginUser(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetailsImpl userDetailsImpl = (UserDetailsImpl) authentication.getPrincipal();

        return jwtService.generateTokens(userDetailsImpl);
    }

    @Transactional
    @Override
    public void registerUser(RegistrationRequest request) {
        if (userCredentialService.existsByUsername(request.username())) {
            throw new ResourceConflictException("Username is already taken!");
        }

        customerService.createEntity(request.customerDto());
        userCredentialService.createUserCredential(request);
    }

    @Override
    public Map<String, String> refreshToken(String token) {
        String username;

        try {
            username = jwtService.extractUsername(token);
        } catch (Exception exception) {
            log.error(LogMessage.EXCEPTION, exception.getMessage());
            throw new BadRequestException("Invalid refresh token!");
        }

        return jwtService.generateTokens(userDetailsService.loadUserByUsername(username));
    }
}
