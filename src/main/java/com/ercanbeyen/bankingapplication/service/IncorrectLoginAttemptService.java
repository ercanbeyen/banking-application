package com.ercanbeyen.bankingapplication.service;

import com.ercanbeyen.bankingapplication.dto.IncorrectLoginAttemptDto;

public interface IncorrectLoginAttemptService {
    void createIncorrectLoginAttempt(IncorrectLoginAttemptDto request);
}
