package com.ercanbeyen.bankingapplication.service;

import jakarta.mail.MessagingException;

import java.io.IOException;

public interface EmailService {
    void sendEmail(String to, String subject, String body);
    void sendEmail(String to, String subject, String fileName, byte[] data) throws MessagingException, IOException;
}
