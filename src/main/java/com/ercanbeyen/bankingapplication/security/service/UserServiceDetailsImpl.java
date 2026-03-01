package com.ercanbeyen.bankingapplication.security.service;

import com.ercanbeyen.bankingapplication.entity.UserCredential;
import com.ercanbeyen.bankingapplication.repository.UserCredentialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class UserServiceDetailsImpl implements UserDetailsService {
    @Autowired
    private UserCredentialRepository userCredentialRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserCredential user = userCredentialRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Credential is not found"));
        return new UserDetailsImpl(user.getUsername(), user.getPassword(), Collections.emptySet());
    }
}
