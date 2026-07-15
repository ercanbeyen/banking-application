package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.message.LogMessage;
import com.ercanbeyen.bankingapplication.service.OtpService;
import com.ercanbeyen.bankingapplication.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {
    private static final String OTP_KEY_PREFIX = "otp:";
    private static final String ATTEMPTS_KEY_PREFIX = "otp_attempts:";
    private static final int MAX_OTP_ATTEMPTS = 3;

    private final StringRedisTemplate redisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generateOtp(String email) {
        String otp = String.format("%06d", secureRandom.nextInt(999999));
        String otpKey = OTP_KEY_PREFIX + email;
        String attemptsKey = ATTEMPTS_KEY_PREFIX + email;

        redisTemplate.opsForValue().set(
                otpKey,
                otp,
                AuthUtil.getOtpValidMinutes(),
                TimeUnit.MINUTES
        );

        redisTemplate.delete(attemptsKey);

        return otp;
    }

    @Override
    public boolean validateOtp(String email, String otp) {
        String otpKey = OTP_KEY_PREFIX + email;
        String attemptsKey = ATTEMPTS_KEY_PREFIX + email;
        String cachedOtp = redisTemplate.opsForValue().get(otpKey);

        if (cachedOtp == null) { // No OTP or expired
            redisTemplate.delete(attemptsKey);
            return false;
        }

        if (cachedOtp.equals(otp)) { // match --> validation success
            redisTemplate.delete(otpKey);
            redisTemplate.delete(attemptsKey);
            return true;
        }

        /* Wrong OTP */
        Long attempts = redisTemplate.opsForValue().increment(attemptsKey);

        if (attempts == null) { // Unknown OTP
            log.warn(LogMessage.RESOURCE_NOT_FOUND, "OTP");
        } else if (attempts == 1) { // set expiry time of otp attempts counter
            redisTemplate.expire(attemptsKey, AuthUtil.getOtpValidMinutes(), TimeUnit.MINUTES);
        } else if (attempts >= MAX_OTP_ATTEMPTS) { // invalidate the OTP
            redisTemplate.delete(otpKey);
            redisTemplate.delete(attemptsKey);
        }

        return false;
    }
}
