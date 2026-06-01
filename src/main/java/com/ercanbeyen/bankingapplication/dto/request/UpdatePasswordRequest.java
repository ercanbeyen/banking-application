package com.ercanbeyen.bankingapplication.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdatePasswordRequest(
        @NotBlank(message = "New password should not be blank")
        String newPassword,
        @NotBlank(message = "Verification password should not be blank")
        String verificationPassword) {

}
