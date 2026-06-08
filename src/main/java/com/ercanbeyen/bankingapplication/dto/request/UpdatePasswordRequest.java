package com.ercanbeyen.bankingapplication.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdatePasswordRequest(
        @NotBlank(message = "New password should not be blank")
        String newPassword,
        @NotBlank(message = "Verification password should not be blank")
        String verificationPassword,
        @NotNull(message = "Password renewal period can not be null")
        Integer passwordRenewalPeriod) {

}
