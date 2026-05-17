package com.ercanbeyen.bankingapplication.repository;

import com.ercanbeyen.bankingapplication.entity.IncorrectLoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IncorrectLoginAttemptRepository extends JpaRepository<IncorrectLoginAttempt, String> {

}
