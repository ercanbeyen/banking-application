package com.ercanbeyen.bankingapplication.security.service;

import com.ercanbeyen.bankingapplication.security.util.JwtUtil;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.MacAlgorithm;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class JwtService {
    private static final MacAlgorithm ALGORITHM = Jwts.SIG.HS256;
    private static final Map<String, String> TOKEN_HEADER = Map.of("typ", "JWT");
    @Value("${jwt.secret}")
    private String jwtSecret;
    @Value("${jwt.accessExpiration}")
    private int accessTokenDuration;
    @Value("${jwt.refreshExpiration}")
    private int refreshTokenDuration;
    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public Map<String, String> generateTokens(UserDetails userDetails) {
        String username = userDetails.getUsername();
        Set<GrantedAuthority> grantedAuthorities = (Set<GrantedAuthority>) userDetails.getAuthorities();
        Map<String, Object> claims = new HashMap<>();

        final String rolePrefix = "ROLE_";
        Predicate<String> authorityStartsWith = authority -> authority.startsWith(rolePrefix);

        Set<String> roles = grantedAuthorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authorityStartsWith)
                .map(authority -> authority.substring(rolePrefix.length()))
                .collect(Collectors.toSet());
        claims.put("roles", roles);

        Set<String> permissions = grantedAuthorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authorityStartsWith.negate())
                .collect(Collectors.toSet());
        claims.put("permissions", permissions);

        Map<String, Date> times = JwtUtil.generateTimes(accessTokenDuration);

        String accessToken = Jwts.builder()
                .header()
                .add(TOKEN_HEADER)
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
                .add(TOKEN_HEADER)
                .and()
                .subject(username)
                .issuedAt(times.get(Claims.ISSUED_AT))
                .expiration(times.get(Claims.EXPIRATION))
                .signWith(key, ALGORITHM)
                .compact();

        return Map.of(JwtUtil.Header.ACCESS_TOKEN_HEADER, accessToken, JwtUtil.Header.REFRESH_TOKEN_HEADER, refreshToken);
    }

    public String extractSubject(String token) {
        return extractClaims(token).getSubject();
    }

    public Date extractExpiration(String token) {
        return extractClaims(token).getExpiration();
    }

    public Date extractIssuedAt(String token) {
        return extractClaims(token).getIssuedAt();
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
