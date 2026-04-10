package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.annotation.RolesRequest;
import com.ercanbeyen.bankingapplication.constant.enums.ERole;
import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.security.util.JwtUtil;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.dto.request.LoginRequest;
import com.ercanbeyen.bankingapplication.dto.request.RegistrationRequest;
import com.ercanbeyen.bankingapplication.dto.response.MessageResponse;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.service.AuthService;
import com.ercanbeyen.bankingapplication.util.CustomerUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/auth")
@Slf4j
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<Void> loginUser(@RequestBody @Valid LoginRequest request, HttpServletResponse response) {
        Map<String, String> tokens = authService.loginUser(request);
        response.addHeader(JwtUtil.Header.ACCESS_TOKEN_HEADER, tokens.get(JwtUtil.Header.ACCESS_TOKEN_HEADER));
        response.addHeader(JwtUtil.Header.REFRESH_TOKEN_HEADER, tokens.get(JwtUtil.Header.REFRESH_TOKEN_HEADER));

        return ResponseEntity.ok().build();
    }

    @PostMapping("/register")
    public ResponseEntity<MessageResponse<String>> registerUser(@RequestBody @Valid RegistrationRequest request) {
        CustomerUtil.checkRequest(request.customerDto());
        authService.registerUser(request);
        MessageResponse<String> response = new MessageResponse<>("User registered successfully!");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String token = JwtUtil.extractToken(request)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, Entity.REFRESH_TOKEN.getValue())));

        Map<String, String> tokens = authService.refreshToken(token);
        response.addHeader(JwtUtil.Header.ACCESS_TOKEN_HEADER, tokens.get(JwtUtil.Header.ACCESS_TOKEN_HEADER));
        response.addHeader(JwtUtil.Header.REFRESH_TOKEN_HEADER, tokens.get(JwtUtil.Header.REFRESH_TOKEN_HEADER));

        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/{username}/roles")
    public ResponseEntity<Set<ERole>> getRoles(@PathVariable("username") String username) {
        return ResponseEntity.ok(authService.getRoles(username));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/users/{username}/roles")
    public ResponseEntity<MessageResponse<String>> updateRoles(@PathVariable("username") String username, @RequestBody @RolesRequest Set<String> roles) {
        authService.updateRoles(username, roles);
        MessageResponse<String> response = new MessageResponse<>("Roles are successfully updated!");
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('ADMIN') OR #username == authentication.principal.username")
    @PatchMapping(value = "/users/{username}/password", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<MessageResponse<String>> updatePassword(@PathVariable("username") @P("username") String username, @RequestBody @NotBlank(message = "Password should not be blank") String password) {
        authService.updatePassword(username, password);
        MessageResponse<String> response = new MessageResponse<>("Password is successfully updated!");
        return ResponseEntity.ok(response);
    }
}
