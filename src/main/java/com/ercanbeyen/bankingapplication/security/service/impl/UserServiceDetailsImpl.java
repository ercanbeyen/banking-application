package com.ercanbeyen.bankingapplication.security.service.impl;

import com.ercanbeyen.bankingapplication.service.UserCredentialService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceDetailsImpl implements UserDetailsService {
    private final UserCredentialService userCredentialService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return UserDetailsImpl.build(userCredentialService.findByUsername(username));
    }
}
