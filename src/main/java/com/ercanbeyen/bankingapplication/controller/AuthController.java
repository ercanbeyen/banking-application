package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.dto.request.LoginRequest;
import com.ercanbeyen.bankingapplication.dto.request.RegistrationRequest;
import com.ercanbeyen.bankingapplication.dto.response.MessageResponse;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@Slf4j
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> loginUser(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.loginUser(request));
    }

    @PostMapping("/register")
    public ResponseEntity<MessageResponse<String>> registerUser(@RequestBody RegistrationRequest request) {
        if (!request.username().equals(request.customerDto().getNationalId())) {
            return ResponseEntity.badRequest().body(new MessageResponse<>("Username should be same with the national id"));
        }

        authService.registerUser(request);

        return ResponseEntity.ok(new MessageResponse<>("User registered successfully!"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refreshToken(@RequestBody Map<String, String> request) {
        String token = Optional.ofNullable(request.get("refreshToken"))
                .orElseThrow(() -> new ResourceNotFoundException("Refresh token is not found"));

        return ResponseEntity.ok(authService.refreshToken(token));
    }
}
