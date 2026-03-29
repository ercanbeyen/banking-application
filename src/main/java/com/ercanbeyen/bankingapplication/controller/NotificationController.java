package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.dto.response.MessageResponse;
import com.ercanbeyen.bankingapplication.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse<String>> deleteNotification(@PathVariable("id") String id) {
        MessageResponse<String> response = new MessageResponse<>(notificationService.deleteNotification(id));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteNotifications(@RequestParam("customer") String nationalId) {
        notificationService.deleteNotifications(nationalId);
        return ResponseEntity.noContent().build();
    }
}
