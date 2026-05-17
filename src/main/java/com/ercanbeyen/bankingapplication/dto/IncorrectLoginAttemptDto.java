package com.ercanbeyen.bankingapplication.dto;

import java.time.LocalDateTime;

public record IncorrectLoginAttemptDto(String username, LocalDateTime dateTime) {

}
