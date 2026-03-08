package com.ercanbeyen.bankingapplication.security.service;

import com.ercanbeyen.bankingapplication.entity.Permission;
import com.ercanbeyen.bankingapplication.entity.Role;
import com.ercanbeyen.bankingapplication.security.util.JwtUtil;
import com.ercanbeyen.bankingapplication.entity.UserCredential;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.service.UserCredentialService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.MacAlgorithm;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtService {
    private static final MacAlgorithm ALGORITHM = Jwts.SIG.HS256;
    @Value("${jwt.secret}")
    private String jwtSecret;
    @Value("${jwt.accessExpiration}")
    private int accessTokenDuration;
    @Value("${jwt.refreshExpiration}")
    private int refreshTokenDuration;
    private SecretKey key;
    private final UserCredentialService userCredentialService;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public Map<String, String> generateTokens(UserDetails userDetails) {
        String username = userDetails.getUsername();
        UserCredential userCredential = userCredentialService.findByUsername(username);
        Set<Role> userCredentialRoles = userCredential.getRoles();
        Map<String, Object> claims = new HashMap<>();

        Set<String> roles = userCredentialRoles.stream()
                .map(role -> role.getName().toString())
                .collect(Collectors.toSet());
        claims.put("roles", roles);

        Set<String> permissions = userCredentialRoles.stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getName)
                .collect(Collectors.toSet());
        claims.put("permissions", permissions);

        Map<String, Date> times = JwtUtil.generateTimes(accessTokenDuration);

        String accessToken = Jwts.builder()
                .header()
                .add(Header.TYPE, Header.JWT_TYPE)
                .and()
                .claims(claims)
                .subject(username)
                .issuedAt(times.get(Claims.ISSUED_AT))
                .expiration(times.get(Claims.EXPIRATION))
                .signWith(key, ALGORITHM)
                .compact();

        times = JwtUtil.generateTimes(refreshTokenDuration);

        String refreshToken = Jwts.builder()
                .header()
                .add(Header.TYPE, Header.JWT_TYPE)
                .and()
                .subject(username)
                .issuedAt(times.get(Claims.ISSUED_AT))
                .expiration(times.get(Claims.EXPIRATION))
                .signWith(key, ALGORITHM)
                .compact();

        return Map.of(JwtUtil.ACCESS_TOKEN_HEADER, accessToken, JwtUtil.REFRESH_TOKEN_HEADER, refreshToken);
    }

    public String extractSubject(String token) {
        return extractClaims(token).getSubject();
    }

    public Date extractExpiration(String token) {
        return extractClaims(token).getExpiration();
    }

    public String extractToken(HttpServletRequest request) {
        return JwtUtil.extractToken(request)
                .orElseThrow(() -> new ResourceNotFoundException("Token does not exist"));
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException exception) {
            log.error("Invalid JWT signature: {}", exception.getMessage());
        } catch (MalformedJwtException exception) {
            log.error("Invalid JWT token: {}", exception.getMessage());
        } catch (ExpiredJwtException exception) {
            log.error("JWT token is expired: {}", exception.getMessage());
        } catch (UnsupportedJwtException exception) {
            log.error("JWT token is unsupported: {}", exception.getMessage());
        } catch (IllegalArgumentException exception) {
            log.error("JWT claims string is empty: {}", exception.getMessage());
        }

        return false;
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
