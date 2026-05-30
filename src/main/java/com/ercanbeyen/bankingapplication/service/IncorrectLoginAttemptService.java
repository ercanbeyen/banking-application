package com.ercanbeyen.bankingapplication.service;

import com.ercanbeyen.bankingapplication.dto.IncorrectLoginAttemptDto;

import java.util.List;

public interface IncorrectLoginAttemptService {
    void createIncorrectLoginAttempt(IncorrectLoginAttemptDto request);
    List<IncorrectLoginAttemptDto> getIncorrectLoginAttempts(String username);
}
