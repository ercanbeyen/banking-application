package com.ercanbeyen.bankingapplication.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class UserCredentialUtil {
    private final String SYSTEM_ADMIN_USERNAME = "11111111111";

    public String getSystemAdminUsername() {
        return SYSTEM_ADMIN_USERNAME;
    }
}
