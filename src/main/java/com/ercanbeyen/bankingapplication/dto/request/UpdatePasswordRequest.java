package com.ercanbeyen.bankingapplication.dto.request;

import com.ercanbeyen.bankingapplication.annotation.PasswordRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdatePasswordRequest(
        @PasswordRequest String newPassword,
        @NotBlank(message = "Verification password should not be blank")
        String verificationPassword,
        @NotNull(message = "Password renewal period can not be null")
        Integer passwordRenewalPeriod) {

}
