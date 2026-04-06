package com.ercanbeyen.bankingapplication.security.service;

import com.ercanbeyen.bankingapplication.dto.NotificationDto;
import com.ercanbeyen.bankingapplication.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class NotificationSecurityService {
    private final NotificationService notificationService;

    public boolean isOwner(String id, Authentication authentication) {
        NotificationDto requestedNotification = notificationService.getNotification(id);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String customerNationalId = userDetails.getUsername();
        return requestedNotification.customerNationalId().equals(customerNationalId);
    }
}
