package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.dto.request.LoginRequest;
import com.ercanbeyen.bankingapplication.dto.request.RegistrationRequest;
import com.ercanbeyen.bankingapplication.dto.response.MessageResponse;
import com.ercanbeyen.bankingapplication.security.service.JwtService;
import com.ercanbeyen.bankingapplication.security.service.UserDetailsImpl;
import com.ercanbeyen.bankingapplication.service.CustomerService;
import com.ercanbeyen.bankingapplication.service.UserCredentialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Slf4j
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final CustomerService customerService;
    private final UserCredentialService userCredentialService;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;

    @PostMapping("/login")
    public Map<String, String> loginUser(@RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetailsImpl userDetailsImpl = (UserDetailsImpl) authentication.getPrincipal();
        log.info("UserDetails: {}", userDetailsImpl);
        log.info("UserDetails - username: {}", userDetailsImpl.getUsername());
        log.info("UserDetails - password: {}", userDetailsImpl.getPassword());
        log.info("UserDetails - authorities: {}", userDetailsImpl.getAuthorities());

        return jwtService.generateTokens(userDetailsImpl);
    }

    @PostMapping("/register")
    public ResponseEntity<MessageResponse<String>> registerUser(@RequestBody RegistrationRequest request) {
        if (!request.username().equals(request.customerDto().getNationalId())) {
            return ResponseEntity.badRequest().body(new MessageResponse<>("Error: Username should be same with the national id"));
        }

        if (userCredentialService.existsByUsername(request.username())) {
            return ResponseEntity.badRequest().body(new MessageResponse<>("Error: Username is already taken!"));
        }

        customerService.createEntity(request.customerDto());
        userCredentialService.createUserCredential(request);

        return ResponseEntity.ok(new MessageResponse<>("User registered successfully!"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refreshToken(@RequestBody Map<String, String> payload) {
        String token = payload.get("refreshToken");

        String username = jwtService.extractUsername(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        return ResponseEntity.ok(jwtService.generateTokens(userDetails));
    }
}
