package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.ERole;
import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.dto.CustomerDto;
import com.ercanbeyen.bankingapplication.security.util.JwtUtil;
import com.ercanbeyen.bankingapplication.constant.message.LogMessage;
import com.ercanbeyen.bankingapplication.dto.request.LoginRequest;
import com.ercanbeyen.bankingapplication.dto.request.RegistrationRequest;
import com.ercanbeyen.bankingapplication.exception.BadRequestException;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import com.ercanbeyen.bankingapplication.security.service.JwtService;
import com.ercanbeyen.bankingapplication.service.AuthService;
import com.ercanbeyen.bankingapplication.service.CustomerService;
import com.ercanbeyen.bankingapplication.service.RefreshTokenService;
import com.ercanbeyen.bankingapplication.service.UserCredentialService;
import com.ercanbeyen.bankingapplication.util.LoggingUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final AuthenticationManager authenticationManager;
    private final CustomerService customerService;
    private final UserCredentialService userCredentialService;
    private final UserDetailsService userDetailsService;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;

    @Transactional
    @Override
    public Map<String, String> loginUser(LoginRequest request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        Map<String, String> tokens = jwtService.generateTokens(userDetails);

        refreshTokenService.revokeAllRefreshTokens(request.username());
        refreshTokenService.createRefreshToken(tokens.get(JwtUtil.Header.REFRESH_TOKEN_HEADER));

        return tokens;
    }

    @Transactional
    @Override
    public void registerUser(RegistrationRequest request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        if (userCredentialService.existsByUsername(request.customerDto().getNationalId())) {
            throw new ResourceConflictException("User is already registered!");
        }

        CustomerDto registeredCustomer = customerService.createEntity(request.customerDto());
        RegistrationRequest registrationRequestForUserCredential = new RegistrationRequest(registeredCustomer, request.password(), request.roles());

        userCredentialService.createUserCredential(registrationRequestForUserCredential);
    }

    @Override
    public Map<String, String> refreshToken(String token) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        String username;

        try {
            username = jwtService.extractSubject(token);
        } catch (Exception exception) {
            log.error(LogMessage.EXCEPTION, exception.getMessage());
            throw new BadRequestException("Invalid " + Entity.REFRESH_TOKEN.getValue() + "!");
        }

        refreshTokenService.verifyExpiration(token);
        refreshTokenService.revokeAllRefreshTokens(username);

        Map<String, String> tokens = jwtService.generateTokens(userDetailsService.loadUserByUsername(username));
        refreshTokenService.createRefreshToken(tokens.get(JwtUtil.Header.REFRESH_TOKEN_HEADER));

        return tokens;
    }

    @Override
    public Set<ERole> getRoles(String username) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());
        return userCredentialService.getRoles(username);
    }

    @Transactional
    @Override
    public void updateRoles(String username, Set<String> roles) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        userCredentialService.updateRoles(username, roles);
        refreshTokenService.revokeAllRefreshTokens(username);
    }
}
