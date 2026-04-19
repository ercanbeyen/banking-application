package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.dto.NotificationDto;
import com.ercanbeyen.bankingapplication.dto.response.MessageResponse;
import com.ercanbeyen.bankingapplication.security.service.NotificationSecurityService;
import com.ercanbeyen.bankingapplication.service.NotificationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class NotificationController {
    private final NotificationService notificationService;
    private final NotificationSecurityService notificationSecurityService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<NotificationDto> sendNotification(@RequestBody @Valid NotificationDto request) {
        return ResponseEntity.ok(notificationService.sendNotification(request).join());
    }

    @PreAuthorize("@notificationSecurityService.isOwner(#notificationId, authentication)")
    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse<String>> deleteNotification(@PathVariable("id") @P("notificationId") String id) {
        notificationService.deleteNotification(id);
        MessageResponse<String> response = new MessageResponse<>(String.format(ResponseMessage.DELETE_SUCCESS, Entity.NOTIFICATION.getValue()));
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("#customerNationalId == authentication.principal.username")
    @DeleteMapping
    public ResponseEntity<Void> deleteNotifications(@RequestParam("customer") @P("customerNationalId") String nationalId) {
        notificationService.deleteNotifications(nationalId);
        return ResponseEntity.noContent().build();
    }
}
