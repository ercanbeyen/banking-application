package com.ercanbeyen.bankingapplication.util;

import com.ercanbeyen.bankingapplication.dto.request.UpdatePasswordRequest;
import com.ercanbeyen.bankingapplication.exception.BadRequestException;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AuthUtil {
    private final int PASSWORD_HISTORY_MAX_SIZE = 3;
    private final int MAX_FAILED_ATTEMPTS = 5;

    public void checkUpdatePasswordRequest(UpdatePasswordRequest request) {
        if (!request.newPassword().equals(request.verificationPassword())) {
            throw new BadRequestException("Passwords must match!");
        }
    }

    public int getPasswordHistoryMaxSize() {
        return PASSWORD_HISTORY_MAX_SIZE;
    }

    public int getMaxFailedAttempts() {
        return MAX_FAILED_ATTEMPTS;
    }
}
