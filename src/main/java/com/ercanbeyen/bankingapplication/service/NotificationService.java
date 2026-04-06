package com.ercanbeyen.bankingapplication.service;

import com.ercanbeyen.bankingapplication.dto.NotificationDto;

import java.util.concurrent.CompletableFuture;

public interface NotificationService {
    CompletableFuture<NotificationDto> sendNotification(NotificationDto request);
    NotificationDto getNotification(String id);
    String deleteNotification(String id);
    void deleteNotifications(String nationalId);
}
