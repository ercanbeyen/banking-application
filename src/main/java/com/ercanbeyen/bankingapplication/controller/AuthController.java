package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.annotation.RolesRequest;
import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.EmailMessage;
import com.ercanbeyen.bankingapplication.dto.CustomerDto;
import com.ercanbeyen.bankingapplication.dto.IncorrectLoginAttemptDto;
import com.ercanbeyen.bankingapplication.dto.request.UpdatePasswordRequest;
import com.ercanbeyen.bankingapplication.dto.request.VerifyOtpRequest;
import com.ercanbeyen.bankingapplication.dto.response.LoginResponse;
import com.ercanbeyen.bankingapplication.entity.Customer;
import com.ercanbeyen.bankingapplication.exception.BadRequestException;
import com.ercanbeyen.bankingapplication.security.util.JwtUtil;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.dto.request.LoginRequest;
import com.ercanbeyen.bankingapplication.dto.request.RegistrationRequest;
import com.ercanbeyen.bankingapplication.dto.response.MessageResponse;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.service.AuthService;
import com.ercanbeyen.bankingapplication.service.CustomerService;
import com.ercanbeyen.bankingapplication.service.EmailService;
import com.ercanbeyen.bankingapplication.service.OtpService;
import com.ercanbeyen.bankingapplication.util.AuthUtil;
import com.ercanbeyen.bankingapplication.util.CustomerUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/auth")
@Slf4j
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final CustomerService customerService;
    private final OtpService otpService;
    private final EmailService emailService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> loginUser(@RequestBody @Valid LoginRequest loginRequest) {
        authService.loginUser(loginRequest);
        CustomerDto customerDto = customerService.getCustomerByNationalId(loginRequest.username());

        String email = customerDto.getEmail();
        String otp = otpService.generateOtp(email);

        String content =  "<p>Hello,"
                + "<br><br>Your verification code for logging into the application: <b>" + otp + "</b>"
                + "<br>This code is valid for " + AuthUtil.getOtpValidMinutes() + " minutes.</p>"
                + EmailMessage.FOOTER;

        emailService.sendEmail(
                email,
                "Verification Code (OTP)",
                content
        );

        LoginResponse response = new LoginResponse(
                true,
                email,
                null,
                null,
                "The password is correct. Please enter the OTP code sent to your email address."
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-login-otp")
    public ResponseEntity<LoginResponse> verifyLoginOtp(@RequestBody VerifyOtpRequest request) {
        String email = request.email();

        if (!otpService.validateOtp(email, request.otp())) {
            throw new BadRequestException("Invalid or expired OTP code!");
        }

        Customer customerDto = customerService.findByEmail(email);
        Map<String, String> tokens = authService.generateTokens(customerDto.getNationalId());

        LoginResponse response = new LoginResponse(
                false,
                email,
                tokens.get(JwtUtil.Header.ACCESS_TOKEN_HEADER),
                tokens.get(JwtUtil.Header.REFRESH_TOKEN_HEADER),
                "Login success! Tokens are generated."
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<MessageResponse<String>> registerUser(@RequestBody @Valid RegistrationRequest request) {
        CustomerDto requestedCustomer = request.customerDto();
        CustomerUtil.checkRequest(requestedCustomer);

        authService.registerUser(request);

        String content = "<p>Hello " + requestedCustomer.getFullName() + ","
                + "<br><br>Welcome to the bank! &#x1F44B</p>"
                + EmailMessage.FOOTER;

        emailService.sendEmail(
                requestedCustomer.getEmail(),
                "Customer Registration",
                content
        );

        MessageResponse<String> response = new MessageResponse<>("User successfully registered!");
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
    public ResponseEntity<Set<String>> getRoles(@PathVariable("username") String username) {
        return ResponseEntity.ok(authService.getRoles(username));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/users/{username}/roles")
    public ResponseEntity<MessageResponse<String>> updateRoles(@PathVariable("username") String username, @RequestBody @RolesRequest Set<String> roles) {
        authService.updateRoles(username, roles);

        MessageResponse<String> response = new MessageResponse<>("Roles are successfully updated!");
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN') OR #username == authentication.principal.username")
    @PatchMapping(value = "/users/{username}/password")
    public ResponseEntity<MessageResponse<String>> updatePassword(@PathVariable("username") @P("username") String username, @RequestBody @Valid UpdatePasswordRequest request) {
        AuthUtil.checkUpdatePasswordRequest(request);
        authService.updatePassword(username, request);

        MessageResponse<String> response = new MessageResponse<>("Password is successfully updated!");
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('READ_DATA') OR #username == authentication.principal.username")
    @GetMapping("/users/{username}/incorrect-login-attempts")
    public ResponseEntity<List<IncorrectLoginAttemptDto>> getIncorrectLoginAttempts(@PathVariable("username") @P("username") String username) {
        return ResponseEntity.ok(authService.getIncorrectLoginAttempts(username));
    }
}
