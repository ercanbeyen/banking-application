package com.ercanbeyen.bankingapplication.security.util;

import lombok.experimental.UtilityClass;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Set;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

@UtilityClass
public class UserDetailsUtil {
    public final String rolePrefix = "ROLE_";

    public String getUsername(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return userDetails.getUsername();
    }

    public Set<String> getRoles(UserDetails userDetails) {
        return userDetails.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authorityStartsWith)
                .map(extractRoleFromAuthority)
                .collect(Collectors.toSet());
    }

    public Set<String> getPermissions(UserDetails userDetails) {
        return userDetails.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authorityStartsWith.negate())
                .collect(Collectors.toSet());
    }

    private final UnaryOperator<String> extractRoleFromAuthority = authority -> authority.substring(rolePrefix.length());
    private final Predicate<String> authorityStartsWith = authority -> authority.startsWith(rolePrefix);
}
