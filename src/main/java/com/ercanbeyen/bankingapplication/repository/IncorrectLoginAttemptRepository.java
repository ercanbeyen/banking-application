package com.ercanbeyen.bankingapplication.repository;

import com.ercanbeyen.bankingapplication.entity.IncorrectLoginAttempt;
import com.ercanbeyen.bankingapplication.entity.UserCredentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncorrectLoginAttemptRepository extends JpaRepository<IncorrectLoginAttempt, String> {
    List<IncorrectLoginAttempt> findTop3ByUserCredentialsOrderByAttemptedAtDesc(UserCredentials userCredentials);
}
