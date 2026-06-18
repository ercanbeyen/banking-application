package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.dto.CustomerDto;
import com.ercanbeyen.bankingapplication.dto.IncorrectLoginAttemptDto;
import com.ercanbeyen.bankingapplication.dto.UserCredentialsDto;
import com.ercanbeyen.bankingapplication.dto.request.PasswordValidationRequest;
import com.ercanbeyen.bankingapplication.dto.request.UpdatePasswordRequest;
import com.ercanbeyen.bankingapplication.entity.Customer;
import com.ercanbeyen.bankingapplication.entity.UserCredentials;
import com.ercanbeyen.bankingapplication.security.util.JwtUtil;
import com.ercanbeyen.bankingapplication.constant.message.LogMessage;
import com.ercanbeyen.bankingapplication.dto.request.LoginRequest;
import com.ercanbeyen.bankingapplication.dto.request.RegistrationRequest;
import com.ercanbeyen.bankingapplication.exception.BadRequestException;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import com.ercanbeyen.bankingapplication.security.service.JwtService;
import com.ercanbeyen.bankingapplication.security.util.UserDetailsUtil;
import com.ercanbeyen.bankingapplication.service.*;
import com.ercanbeyen.bankingapplication.util.AuthUtil;
import com.ercanbeyen.bankingapplication.util.LoggingUtil;
import com.ercanbeyen.bankingapplication.util.TimeUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;

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
    private final UserRevocationService userRevocationService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Map<String, String> loginUser(LoginRequest loginRequest) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        UserCredentials userCredentials = userCredentialsService.findByUsername(loginRequest.username());
        userCredentialsService.checkLockStatus(userCredentials);

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.username(),
                            loginRequest.password()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            userCredentialsService.loginSucceeded(userCredentials.getUsername());

            Map<String, String> tokens = jwtService.generateTokens(userDetails);

            refreshTokenService.revokeAllRefreshTokens(loginRequest.username());
            refreshTokenService.createRefreshToken(tokens.get(JwtUtil.Header.REFRESH_TOKEN_HEADER));

            return tokens;
        } catch (BadCredentialsException exception) {
            log.error("{}!", Entity.INCORRECT_LOGIN_ATTEMPT.getValue());

            userCredentialsService.loginFailed(loginRequest.username());

            if (!userCredentials.isAccountNonLocked()) {
                userRevocationService.revokeAllTokensForUser(userCredentials.getUsername());
                refreshTokenService.revokeAllRefreshTokens(userCredentials.getUsername());
            }

            IncorrectLoginAttemptDto incorrectLoginAttemptRequest = new IncorrectLoginAttemptDto(loginRequest.username(), TimeUtil.getTurkeyDateTime());
            incorrectLoginAttemptService.createIncorrectLoginAttempt(incorrectLoginAttemptRequest);

            throw exception;
        }
    }

    @Transactional
    @Override
    public void registerUser(RegistrationRequest request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        CustomerDto requestedCustomer = request.customerDto();
        String username = requestedCustomer.getNationalId();
        String password = request.password();

        if (userCredentialsService.existsByUsername(username)) {
            log.error(LogMessage.RESOURCE_NOT_UNIQUE, Entity.USER_CREDENTIALS.getValue());
            throw new ResourceConflictException("User is already registered!");
        }

        PasswordValidationRequest passwordValidationRequest = new PasswordValidationRequest(password, username, requestedCustomer.getPhoneNumber(), requestedCustomer.getBirthDate());
        validatePassword(passwordValidationRequest);

        CustomerDto registeredCustomer = customerService.createEntity(requestedCustomer);
        UserCredentialsDto userCredentialRequest = new UserCredentialsDto(
                registeredCustomer.getNationalId(),
                registeredCustomer.getId(),
                password,
                AuthUtil.getDefaultPasswordRenewalPeriod(),
                request.roles()
        );

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
    public Set<String> getRoles(String username) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());
        return UserDetailsUtil.getRoles(userDetailsService.loadUserByUsername(username));
    }

    @Transactional
    @Override
    public void updateRoles(String username, Set<String> roles) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());
        userCredentialsService.updateRoles(username, roles);
        refreshTokenService.revokeAllRefreshTokens(username);
    }

    @Override
    public void updatePassword(String username, UpdatePasswordRequest request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        String newPassword = request.newPassword();

        if (passwordEncoder.matches(newPassword, userDetails.getPassword())) {
            throw new BadRequestException(ResponseMessage.PASSWORD_SHOULD_BE_DIFFERENT);
        }

        Customer customer = customerService.findByNationalId(username);

        PasswordValidationRequest passwordValidationRequest = new PasswordValidationRequest(newPassword, username, customer.getPhoneNumber(), customer.getBirthDate());
        validatePassword(passwordValidationRequest);

        userCredentialsService.updatePassword(username, request);
        refreshTokenService.revokeAllRefreshTokens(username);
    }

    @Override
    public List<IncorrectLoginAttemptDto> getIncorrectLoginAttempts(String username) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());
        return incorrectLoginAttemptService.getIncorrectLoginAttempts(username);
    }

    private void validatePassword(PasswordValidationRequest request) {
        String password = request.password();

        if (request.phoneNumber().contains(password)) {
            throw new BadRequestException("Password should not contain phone number!");
        }

        if (request.nationalId().contains(password)) {
            throw new BadRequestException("Password should not contain national id!");
        }

        checkBirthDate(password, request.birthDate());
    }

    private static void checkBirthDate(String password, LocalDate birthDate) {
        IntFunction<String> formatLocalDateValue = localDateValue -> {
            if (localDateValue < 10) {
                return "0" + localDateValue;
            } else {
                return String.valueOf(localDateValue);
            }
        };

        String year = formatLocalDateValue.apply(birthDate.getYear());
        String month = formatLocalDateValue.apply(birthDate.getMonthValue());
        String day = formatLocalDateValue.apply(birthDate.getDayOfMonth());

        List<String> birthDateCombinations = List.of(
                /* year & month combinations */
                year + month,
                month + year,
                /* month & day combinations */
                month + day,
                day + month,
                /* year & day combinations */
                year + day,
                day + year
        );

        if (birthDateCombinations.contains(password)) {
            throw new BadRequestException("Password should not contain birth date!");
        }
    }
}
