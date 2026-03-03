package com.ercanbeyen.bankingapplication.repository;

import com.ercanbeyen.bankingapplication.entity.RefreshToken;
import com.ercanbeyen.bankingapplication.entity.UserCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    List<RefreshToken> findAllByUserCredential(UserCredential userCredential);
}
