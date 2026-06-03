package com.ercanbeyen.bankingapplication.util;

import com.ercanbeyen.bankingapplication.dto.request.UpdatePasswordRequest;
import com.ercanbeyen.bankingapplication.exception.BadRequestException;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AuthUtil {
    public void checkUpdatePasswordRequest(UpdatePasswordRequest request) {
        if (!request.newPassword().equals(request.verificationPassword())) {
            throw new BadRequestException("Passwords must match!");
        }
    }
}
