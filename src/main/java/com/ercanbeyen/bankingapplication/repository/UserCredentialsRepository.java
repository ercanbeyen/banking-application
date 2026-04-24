package com.ercanbeyen.bankingapplication.repository;

import com.ercanbeyen.bankingapplication.entity.UserCredentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserCredentialsRepository extends JpaRepository<UserCredentials, Long> {
    boolean existsByUsername(String username);
    Optional<UserCredentials> findByUsername(String username);
}
