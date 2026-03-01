package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.dto.request.RegistrationRequest;
import com.ercanbeyen.bankingapplication.entity.UserCredential;
import com.ercanbeyen.bankingapplication.repository.UserCredentialRepository;
import com.ercanbeyen.bankingapplication.service.UserCredentialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserCredentialServiceImpl implements UserCredentialService {
    private final UserCredentialRepository userCredentialRepository;
    private final PasswordEncoder passwordEncoder;

    public void createUserCredential(RegistrationRequest request) {
        UserCredential userCredential = new UserCredential();

        userCredential.setUsername(request.username());
        userCredential.setPassword(passwordEncoder.encode(request.password()));

        userCredentialRepository.save(userCredential);
    }

    public boolean existsByUsername(String username) {
        return userCredentialRepository.existsByUsername(username);
    }
}
