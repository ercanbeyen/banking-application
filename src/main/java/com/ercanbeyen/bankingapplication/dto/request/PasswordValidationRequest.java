package com.ercanbeyen.bankingapplication.dto.request;

import java.time.LocalDate;

public record PasswordValidationRequest(String password, String nationalId, String phoneNumber, LocalDate birthDate) {

}
