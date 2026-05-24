package com.ercanbeyen.bankingapplication.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenBlackListService {
    private final StringRedisTemplate redisTemplate;

    public void blacklistToken(String token, long expirationMillis) {
        if (expirationMillis > 0) {
            redisTemplate.opsForValue().set(
                    "blacklist:" + token,
                    "true",
                    expirationMillis,
                    TimeUnit.MILLISECONDS
            );
        }
    }

    public boolean isTokenBlacklisted(String token) {
        return redisTemplate.hasKey("blacklist:" + token);
    }
}
