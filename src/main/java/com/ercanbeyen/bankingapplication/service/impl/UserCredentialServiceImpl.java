package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.ERole;
import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.dto.CustomerDto;
import com.ercanbeyen.bankingapplication.dto.request.RegistrationRequest;
import com.ercanbeyen.bankingapplication.model.Role;
import com.ercanbeyen.bankingapplication.model.UserCredential;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.repository.UserCredentialRepository;
import com.ercanbeyen.bankingapplication.service.RoleService;
import com.ercanbeyen.bankingapplication.service.UserCredentialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserCredentialServiceImpl implements UserCredentialService {
    private final UserCredentialRepository userCredentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleService roleService;

    @Override
    public void createUserCredential(RegistrationRequest request) {
        CustomerDto requestedCustomer = request.customerDto();

        UserCredential userCredential = new UserCredential();
        userCredential.setCustomerId(requestedCustomer.getId());
        userCredential.setUsername(requestedCustomer.getNationalId());
        userCredential.setPassword(passwordEncoder.encode(request.password()));

        Set<Role> roles = getRequestedRoles(request.roles());
        userCredential.setRoles(roles);

        userCredentialRepository.save(userCredential);
    }

    @Override
    public UserCredential findByUsername(String username) {
        return userCredentialRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, Entity.USER_CREDENTIAL.getValue())));
    }

    @Override
    public Set<ERole> getRoles(String username) {
        return findByUsername(username)
                .getRoles()
                .stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
    }

    @Override
    public void updateRoles(String username, Set<String> request) {
        Set<Role> roles = getRequestedRoles(request);
        UserCredential userCredential = findByUsername(username);
        userCredential.setRoles(roles);
        userCredentialRepository.save(userCredential);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userCredentialRepository.existsByUsername(username);
    }

    private Set<Role> getRequestedRoles(Set<String> requestedRoles) {
        return requestedRoles.stream()
                .map(requestedRole -> roleService.findByName(ERole.valueOf(requestedRole)))
                .collect(Collectors.toSet());
    }
}
