package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.util.EmailUtil;
import com.ercanbeyen.bankingapplication.exception.InternalServerErrorException;
import com.ercanbeyen.bankingapplication.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
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
    public void sendEmail(String to, String subject, String content) {
        executeMailSender(to, subject, content, null);
    }

    @Async
    @Override
    public void sendEmail(String to, String subject, String fileName, byte[] data) {
        File file = new File(fileName);

        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            outputStream.write(data);
        } catch (FileNotFoundException exception) {
            throw new InternalServerErrorException("Failed to open file: " + exception.getMessage());
        } catch (IOException exception) {
            throw new InternalServerErrorException("Failed to process file: " + exception.getMessage());
        }

        String content = EmailUtil.constructContent(null, "Here is your requested attachment.");

        executeMailSender(to, subject, content, file);
    }

    private void executeMailSender(String to, String subject, String content, File file) {
        MimeMessagePreparator preparator = mimeMessage -> {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);

            if (file != null) {
                helper.addAttachment(file.getName(), file);
            }
        };

        try {
            mailSender.send(preparator);
        } catch (MailException exception) {
            throw new InternalServerErrorException("Failed to send email: " + exception.getMessage());
        }
    }
}
