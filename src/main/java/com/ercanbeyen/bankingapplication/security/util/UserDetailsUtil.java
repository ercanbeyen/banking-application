package com.ercanbeyen.bankingapplication.security.util;

import com.ercanbeyen.bankingapplication.constant.enums.ERole;
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
    private final String ROLE_PREFIX = "ROLE_";

    public String getUsername(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return userDetails.getUsername();
    }

    public String getRole(ERole eRole) {
        return ROLE_PREFIX + eRole.toString();
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

    private final UnaryOperator<String> extractRoleFromAuthority = authority -> authority.substring(ROLE_PREFIX.length());
    private final Predicate<String> authorityStartsWith = authority -> authority.startsWith(ROLE_PREFIX);
}
