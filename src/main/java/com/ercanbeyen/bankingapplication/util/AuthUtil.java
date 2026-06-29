package com.ercanbeyen.bankingapplication.util;

import com.ercanbeyen.bankingapplication.dto.request.UpdatePasswordRequest;
import com.ercanbeyen.bankingapplication.exception.BadRequestException;
import lombok.experimental.UtilityClass;

import java.util.Collections;
import java.util.List;

@UtilityClass
public class AuthUtil {
    private final int PASSWORD_HISTORY_MAX_SIZE = 3;
    private final int MAX_FAILED_ATTEMPTS = 5;
    private final int OTP_VALID_MINUTES = 5;
    private final List<Integer> passwordRenewalPeriods = List.of(1, 3, 6);

    public void checkUpdatePasswordRequest(UpdatePasswordRequest request) {
        if (!request.newPassword().equals(request.verificationPassword())) {
            throw new BadRequestException("Passwords must match!");
        }

        if (!passwordRenewalPeriods.contains(request.passwordRenewalPeriod())) {
            throw new BadRequestException("Invalid password renewal period!");
        }
    }

    public int getPasswordHistoryMaxSize() {
        return PASSWORD_HISTORY_MAX_SIZE;
    }

    public int getMaxFailedAttempts() {
        return MAX_FAILED_ATTEMPTS;
    }

    public int getDefaultPasswordRenewalPeriod() {
        return Collections.max(passwordRenewalPeriods);
    }

    public static int getOtpValidMinutes() {
        return OTP_VALID_MINUTES;
    }
}
