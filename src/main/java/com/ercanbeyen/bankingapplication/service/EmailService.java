package com.ercanbeyen.bankingapplication.service;

public interface EmailService {
    void sendEmail(String to, String subject, String body);
}
