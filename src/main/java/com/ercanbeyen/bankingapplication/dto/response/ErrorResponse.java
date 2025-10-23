package com.ercanbeyen.bankingapplication.dto.response;

import java.time.LocalDateTime;

public record ErrorResponse(int httpStatus, String message, LocalDateTime createdAt) {

}
