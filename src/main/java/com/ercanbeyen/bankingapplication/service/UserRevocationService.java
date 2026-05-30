package com.ercanbeyen.bankingapplication.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UserRevocationService {
    private static final String REDIS_KEY_PREFIX = "invalidated_user:";
    public static final long LOCK_TIME_DURATION_MINUTES = 30;

    private final StringRedisTemplate redisTemplate;

    public void revokeAllTokensForUser(String username) {
        String key = REDIS_KEY_PREFIX + username;
        String revokedAt = String.valueOf(System.currentTimeMillis());

        redisTemplate.opsForValue().set(
                key,
                revokedAt,
                LOCK_TIME_DURATION_MINUTES,
                TimeUnit.MINUTES
        );
    }

    public boolean isTokenRevoked(String username, long tokenIssuedAtMillis) {
        String key = REDIS_KEY_PREFIX + username;
        String revokedAt = redisTemplate.opsForValue().get(key);

        if (revokedAt == null) { // token is not revoked
            return false;
        }

        long revokedAtMillis = Long.parseLong(revokedAt);

        return tokenIssuedAtMillis < revokedAtMillis; // if token was generated before the account was locked, then token is revoked
    }
}
