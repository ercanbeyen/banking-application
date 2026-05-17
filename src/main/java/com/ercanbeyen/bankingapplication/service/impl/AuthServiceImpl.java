package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.ERole;
import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.dto.CustomerDto;
import com.ercanbeyen.bankingapplication.dto.IncorrectLoginAttemptDto;
import com.ercanbeyen.bankingapplication.dto.UserCredentialsDto;
import com.ercanbeyen.bankingapplication.security.util.JwtUtil;
import com.ercanbeyen.bankingapplication.constant.message.LogMessage;
import com.ercanbeyen.bankingapplication.dto.request.LoginRequest;
import com.ercanbeyen.bankingapplication.dto.request.RegistrationRequest;
import com.ercanbeyen.bankingapplication.exception.BadRequestException;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import com.ercanbeyen.bankingapplication.security.service.JwtService;
import com.ercanbeyen.bankingapplication.service.*;
import com.ercanbeyen.bankingapplication.util.LoggingUtil;
import com.ercanbeyen.bankingapplication.util.TimeUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
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
    private final UserCredentialsService userCredentialsService;
    private final UserDetailsService userDetailsService;
    private final RefreshTokenService refreshTokenService;
    private final IncorrectLoginAttemptService incorrectLoginAttemptService;
    private final JwtService jwtService;

    @Override
    public Map<String, String> loginUser(LoginRequest request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        UserDetails userDetails;

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.username(),
                            request.password()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            userDetails = (UserDetails) authentication.getPrincipal();
        } catch (AuthenticationException exception) {
            log.error("Incorrect Login Attempt!");

            IncorrectLoginAttemptDto incorrectLoginAttemptRequest = new IncorrectLoginAttemptDto(request.username(), TimeUtil.getTurkeyDateTime());
            incorrectLoginAttemptService.createIncorrectLoginAttempt(incorrectLoginAttemptRequest);

            throw exception;
        }

        Map<String, String> tokens = jwtService.generateTokens(userDetails);

        refreshTokenService.revokeAllRefreshTokens(request.username());
        refreshTokenService.createRefreshToken(tokens.get(JwtUtil.Header.REFRESH_TOKEN_HEADER));

        return tokens;
    }

    @Transactional
    @Override
    public void registerUser(RegistrationRequest request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        if (userCredentialsService.existsByUsername(request.customerDto().getNationalId())) {
            throw new ResourceConflictException("User is already registered!");
        }

        CustomerDto registeredCustomer = customerService.createEntity(request.customerDto());
        UserCredentialsDto userCredentialRequest = new UserCredentialsDto(registeredCustomer.getNationalId(), registeredCustomer.getId(), request.password(), request.roles());

        userCredentialsService.createUserCredentials(userCredentialRequest);
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
        return userCredentialsService.getRoles(username);
    }

    @Transactional
    @Override
    public void updateRoles(String username, Set<String> roles) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        userCredentialsService.updateRoles(username, roles);
        refreshTokenService.revokeAllRefreshTokens(username);
    }

    @Override
    public void updatePassword(String username, String password) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        userCredentialsService.updatePassword(username, password);
        refreshTokenService.revokeAllRefreshTokens(username);
    }
}
