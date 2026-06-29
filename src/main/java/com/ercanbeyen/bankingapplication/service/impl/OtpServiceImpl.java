package com.ercanbeyen.bankingapplication.service.impl;

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

    private final StringRedisTemplate redisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generateOtp(String email) {
        String otp = String.format("%06d", secureRandom.nextInt(999999));
        String otpKey = OTP_KEY_PREFIX + email;

        redisTemplate.opsForValue().set(
                otpKey,
                otp,
                AuthUtil.getOtpValidMinutes(),
                TimeUnit.MINUTES
        );

        return otp;
    }

    @Override
    public boolean validateOtp(String email, String otp) {
        String otpKey = OTP_KEY_PREFIX + email;
        String cachedOtp = redisTemplate.opsForValue().get(otpKey);

        if (cachedOtp == null) { // No OTP or expired
            return false;
        }

        if (cachedOtp.equals(otp)) { // match --> validation success
            redisTemplate.delete(otpKey);
            return true;
        }

        return false;
    }
}
