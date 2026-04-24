package com.ercanbeyen.bankingapplication.security.util;

import lombok.experimental.UtilityClass;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

@UtilityClass
public class UserDetailsUtil {
    public String getUsername(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return userDetails.getUsername();
    }
}
