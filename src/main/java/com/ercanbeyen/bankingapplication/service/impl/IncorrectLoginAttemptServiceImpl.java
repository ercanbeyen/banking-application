package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.LogMessage;
import com.ercanbeyen.bankingapplication.dto.IncorrectLoginAttemptDto;
import com.ercanbeyen.bankingapplication.entity.IncorrectLoginAttempt;
import com.ercanbeyen.bankingapplication.entity.UserCredentials;
import com.ercanbeyen.bankingapplication.mapper.IncorrectLoginAttemptMapper;
import com.ercanbeyen.bankingapplication.repository.IncorrectLoginAttemptRepository;
import com.ercanbeyen.bankingapplication.service.IncorrectLoginAttemptService;
import com.ercanbeyen.bankingapplication.service.UserCredentialsService;
import com.ercanbeyen.bankingapplication.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class IncorrectLoginAttemptServiceImpl implements IncorrectLoginAttemptService {
    private final IncorrectLoginAttemptRepository incorrectLoginAttemptRepository;
    private final IncorrectLoginAttemptMapper incorrectLoginAttemptMapper;
    private final UserCredentialsService userCredentialsService;

    @Override
    public void createIncorrectLoginAttempt(IncorrectLoginAttemptDto request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        UserCredentials userCredentials = userCredentialsService.findByUsername(request.username());

        IncorrectLoginAttempt incorrectLoginAttempt = incorrectLoginAttemptMapper.dtoToEntity(request);
        incorrectLoginAttempt.setUserCredentials(userCredentials);
        IncorrectLoginAttempt savedIncorrectLoginAttempt = incorrectLoginAttemptRepository.save(incorrectLoginAttempt);

        log.info(LogMessage.RESOURCE_CREATE_SUCCESS, Entity.INCORRECT_LOGIN_ATTEMPT.getValue(), savedIncorrectLoginAttempt.getId());
    }

    @Override
    public List<IncorrectLoginAttemptDto> getIncorrectLoginAttempts(String username) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        UserCredentials userCredentials = userCredentialsService.findByUsername(username);

        return incorrectLoginAttemptRepository.findTop3ByUserCredentialsOrderByAttemptedAtDesc(userCredentials)
                .stream()
                .map(incorrectLoginAttemptMapper::entityToDto)
                .toList();
    }
}
