package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.ERole;
import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.dto.request.RegistrationRequest;
import com.ercanbeyen.bankingapplication.entity.Role;
import com.ercanbeyen.bankingapplication.entity.UserCredential;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.repository.UserCredentialRepository;
import com.ercanbeyen.bankingapplication.service.RoleService;
import com.ercanbeyen.bankingapplication.service.UserCredentialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserCredentialServiceImpl implements UserCredentialService {
    private final UserCredentialRepository userCredentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleService roleService;

    @Override
    public void createUserCredential(RegistrationRequest request) {
        UserCredential userCredential = new UserCredential();

        userCredential.setUsername(request.username());
        userCredential.setPassword(passwordEncoder.encode(request.password()));
        Set<Role> roles = new HashSet<>();

        request.roles().forEach(requestedRole -> {
            Role role = roleService.findByName(ERole.valueOf(requestedRole));
            roles.add(role);
        });

        userCredential.setRoles(roles);
        userCredentialRepository.save(userCredential);
    }

    @Override
    public UserCredential findByUsername(String username) {
        return userCredentialRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, Entity.USER_CREDENTIAL.getValue())));
    }

    @Override
    public boolean existsByUsername(String username) {
        return userCredentialRepository.existsByUsername(username);
    }
}
