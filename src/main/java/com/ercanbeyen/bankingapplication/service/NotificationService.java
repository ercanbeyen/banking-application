package com.ercanbeyen.bankingapplication.service;

import com.ercanbeyen.bankingapplication.dto.NotificationDto;

public interface NotificationService {
    void sendNotification(NotificationDto request);
    String deleteNotification(String id);
    void deleteNotifications(String nationalId);
}
