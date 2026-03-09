package com.ercanbeyen.bankingapplication.security.util;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpHeaders;

import java.util.Date;
import java.util.Map;
import java.util.Optional;

@UtilityClass
public class JwtUtil {
    private final String TOKEN_TYPE = "Bearer";

    public Optional<String> extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        return Optional.ofNullable(authHeader).isPresent() && authHeader.startsWith(JwtUtil.TOKEN_TYPE + " ")
                ? Optional.of(authHeader.substring(JwtUtil.TOKEN_TYPE.length() + 1))
                : Optional.empty();
    }

    public Map<String, Date> generateTimes(int tokenDuration) {
        Date issuedAt = new Date();
        Date expiration = calculateExpiration(issuedAt, tokenDuration);

        return Map.of(
                Claims.ISSUED_AT, issuedAt,
                Claims.EXPIRATION, expiration
        );
    }

    private Date calculateExpiration(Date issuedAt, int tokenDuration) {
        return new Date(issuedAt.getTime() + tokenDuration);
    }

    @UtilityClass
    public class Header {
        public final String ACCESS_TOKEN_HEADER = "access_token";
        public final String REFRESH_TOKEN_HEADER = "refresh_token";
    }
}
