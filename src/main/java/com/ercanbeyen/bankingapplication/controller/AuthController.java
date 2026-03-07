package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.security.util.JwtUtil;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.dto.request.LoginRequest;
import com.ercanbeyen.bankingapplication.dto.request.RegistrationRequest;
import com.ercanbeyen.bankingapplication.dto.response.MessageResponse;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@Slf4j
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<Void> loginUser(@RequestBody LoginRequest request, HttpServletResponse response) {
        Map<String, String> tokens = authService.loginUser(request);
        response.addHeader(JwtUtil.ACCESS_TOKEN_HEADER, tokens.get(JwtUtil.ACCESS_TOKEN_HEADER));
        response.addHeader(JwtUtil.REFRESH_TOKEN_HEADER, tokens.get(JwtUtil.REFRESH_TOKEN_HEADER));

        return ResponseEntity.ok().build();
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
    public ResponseEntity<Void> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String token = JwtUtil.extractToken(request)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, Entity.REFRESH_TOKEN.getValue())));

        Map<String, String> tokens = authService.refreshToken(token);
        response.addHeader(JwtUtil.ACCESS_TOKEN_HEADER, tokens.get(JwtUtil.ACCESS_TOKEN_HEADER));
        response.addHeader(JwtUtil.REFRESH_TOKEN_HEADER, tokens.get(JwtUtil.REFRESH_TOKEN_HEADER));

        return ResponseEntity.ok().build();
    }
}
