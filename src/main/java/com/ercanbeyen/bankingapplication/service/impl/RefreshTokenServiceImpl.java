package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.enums.TokenStatus;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.model.RefreshToken;
import com.ercanbeyen.bankingapplication.model.UserCredential;
import com.ercanbeyen.bankingapplication.exception.BadRequestException;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.repository.RefreshTokenRepository;
import com.ercanbeyen.bankingapplication.security.service.JwtService;
import com.ercanbeyen.bankingapplication.service.RefreshTokenService;
import com.ercanbeyen.bankingapplication.service.UserCredentialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final UserCredentialService userCredentialService;

    @Override
    public void createRefreshToken(String token) {
        String username = jwtService.extractSubject(token);
        UserCredential userCredential = userCredentialService.findByUsername(username);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserCredential(userCredential);

        Date expiryDate = jwtService.extractExpiration(token);
        refreshToken.setExpiryDate(expiryDate.toInstant());

        refreshToken.setToken(token);
        refreshToken.setStatus(TokenStatus.ACTIVE);

        refreshTokenRepository.save(refreshToken);
    }

    @Override
    public RefreshToken findByToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, Entity.REFRESH_TOKEN.getValue())));
    }

    @Override
    public void verifyExpiration(String token) {
        RefreshToken refreshToken = findByToken(token);
        String entityValue = Entity.REFRESH_TOKEN.getValue();

        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new BadRequestException(entityValue + " expired. Please login again");
        }

        if (refreshToken.getStatus() == TokenStatus.REVOKED) {
            throw new BadRequestException(entityValue + " revoked. Please login again");
        }
    }

    @Override
    public void revokeAllRefreshTokens(String username) {
        UserCredential userCredential = userCredentialService.findByUsername(username);
        List<RefreshToken> refreshTokens = refreshTokenRepository.findAllByUserCredential(userCredential);

        if (refreshTokens.isEmpty()) {
            log.info("There is no active {} for the user", Entity.REFRESH_TOKEN.getValue());
            return;
        }

        refreshTokens.forEach(refreshToken -> refreshToken.setStatus(TokenStatus.REVOKED));
        refreshTokenRepository.saveAll(refreshTokens);
    }
}
