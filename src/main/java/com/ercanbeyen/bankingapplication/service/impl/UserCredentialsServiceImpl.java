package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.ERole;
import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.LogMessage;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.dto.UserCredentialsDto;
import com.ercanbeyen.bankingapplication.entity.Role;
import com.ercanbeyen.bankingapplication.entity.UserCredentials;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.repository.UserCredentialsRepository;
import com.ercanbeyen.bankingapplication.service.RoleService;
import com.ercanbeyen.bankingapplication.service.UserRevocationService;
import com.ercanbeyen.bankingapplication.service.UserCredentialsService;
import com.ercanbeyen.bankingapplication.util.LoggingUtil;
import com.ercanbeyen.bankingapplication.util.TimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserCredentialsServiceImpl implements UserCredentialsService {
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int PASSWORD_HISTORY_MAX_SIZE = 3;

    private final UserCredentialsRepository userCredentialsRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleService roleService;

    @Override
    public void createUserCredentials(UserCredentialsDto request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        UserCredentials userCredentials = new UserCredentials();
        userCredentials.setCustomerId(request.customerId());
        userCredentials.setUsername(request.username());

        String password = passwordEncoder.encode(request.password());
        userCredentials.setPassword(password);
        userCredentials.getPasswordHistory().add(password);

        userCredentials.setUpdatePasswordAt(TimeUtil.getTurkeyDateTime());

        Set<Role> roles = getRequestedRoles(request.roles());
        userCredentials.setRoles(roles);

        userCredentialsRepository.save(userCredentials);
    }

    @Override
    public void loginSucceeded(String username) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        userCredentialsRepository.findByUsername(username)
                .ifPresent(userCredentials -> {
                    userCredentials.setFailedAttempt(0);
                    userCredentials.setAccountNonLocked(true);
                    userCredentials.setLockAt(null);

                    userCredentialsRepository.save(userCredentials);
                });
    }

    @Override
    public void loginFailed(String username) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        userCredentialsRepository.findByUsername(username)
                .ifPresent(userCredentials -> {
                    int newAttempts = userCredentials.getFailedAttempt() + 1;
                    userCredentials.setFailedAttempt(newAttempts);

                    if (newAttempts >= MAX_FAILED_ATTEMPTS) {
                        userCredentials.setAccountNonLocked(false);
                        userCredentials.setLockAt(LocalDateTime.now());
                    }

                    userCredentialsRepository.save(userCredentials);
                });
    }

    @Override
    public void checkLockStatus(UserCredentials userCredentials) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        if (userCredentials.isAccountNonLocked()) {
            log.warn("Account has not been locked!");
            return;
        }

        if (userCredentials.getLockAt() != null) { // Account has been locked before
            LocalDateTime lockExpiryTime = userCredentials.getLockAt().plusMinutes(UserRevocationService.LOCK_TIME_DURATION_MINUTES);

            if (LocalDateTime.now().isAfter(lockExpiryTime)) { // The lock period has expired, open the account
                userCredentials.setAccountNonLocked(true);
                userCredentials.setFailedAttempt(0);
                userCredentials.setLockAt(null);

                userCredentialsRepository.save(userCredentials);

                return;
            }
        }

        throw new LockedException("Your account has been locked due to too many failed attempts. Duration: " + UserRevocationService.LOCK_TIME_DURATION_MINUTES + " minutes.");
    }

    @Override
    public void updateRoles(String username, Set<String> request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Set<Role> roles = getRequestedRoles(request);
        UserCredentials userCredentials = findByUsername(username);
        userCredentials.setRoles(roles);

        userCredentialsRepository.save(userCredentials);
    }

    @Override
    public void updatePassword(String username, String password) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        UserCredentials userCredentials = findByUsername(username);
        checkPasswordHistory(password, userCredentials.getPasswordHistory());

        String encodedPassword = passwordEncoder.encode(password);
        userCredentials.setPassword(encodedPassword);
        userCredentials.getPasswordHistory().add(encodedPassword);

        userCredentials.setUpdatePasswordAt(TimeUtil.getTurkeyDateTime());

        userCredentialsRepository.save(userCredentials);
    }

    @Override
    public UserCredentials findByUsername(String username) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());
        return userCredentialsRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, Entity.USER_CREDENTIALS.getValue())));
    }

    @Override
    public boolean existsByUsername(String username) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());
        return userCredentialsRepository.existsByUsername(username);
    }

    private void checkPasswordHistory(String password, List<String> passwordHistory) {
        List<String> last3Passwords = passwordHistory.stream()
                .skip(Math.max(0, passwordHistory.size() - PASSWORD_HISTORY_MAX_SIZE))
                .toList();

        for (String passwordInHistory : last3Passwords) {
            if (passwordEncoder.matches(password, passwordInHistory)) {
                throw new ResourceConflictException("New password should be different from your last " + PASSWORD_HISTORY_MAX_SIZE + " passwords!");
            }
        }
    }

    private Set<Role> getRequestedRoles(Set<String> roles) {
        return roles.stream()
                .map(role -> roleService.findByName(ERole.valueOf(role)))
                .collect(Collectors.toSet());
    }
}
