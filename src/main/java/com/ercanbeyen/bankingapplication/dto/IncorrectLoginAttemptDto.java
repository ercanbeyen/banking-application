package com.ercanbeyen.bankingapplication.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

public record IncorrectLoginAttemptDto(
        String username,
        @JsonFormat(
                shape = JsonFormat.Shape.STRING,
                pattern = "yyyy-MM-dd HH:mm:ss",
                timezone = "UTC"
        )
        Instant attemptedAt) {

}
