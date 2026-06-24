package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;

@RequiredArgsConstructor
@Service
public class EmailServiceImpl implements EmailService {
    @Value("${email.from}")
    private String from;
    private final JavaMailSender mailSender;

    @Async
    @Override
    public void sendEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
    }

    @Async
    @Override
    public void sendEmail(String to, String subject, String fileName, byte[] data) throws MessagingException, IOException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);

        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText("Here is your requested attachment.");

        File file = new File(fileName);

        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            outputStream.write(data);
        }

        helper.addAttachment(file.getName(), file);

        mailSender.send(mimeMessage);
    }
}
