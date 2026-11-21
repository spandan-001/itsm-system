package com.itsm.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.itsm.dto.NotificationResponse;
import com.itsm.service.notification.NotificationService;

/**
 * Notification inbox. Until sign-in is integrated the recipient is passed as
 * {@code ?email=}; afterwards it should come from the signed-in user.
 */
@RestController
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/api/notifications")
    public List<NotificationResponse> forRecipient(@RequestParam String email) {
        return notificationService.forRecipient(email).stream().map(NotificationResponse::from).toList();
    }

    @GetMapping("/api/notifications/unread-count")
    public Map<String, Long> unreadCount(@RequestParam String email) {
        return Map.of("unread", notificationService.unreadCount(email));
    }

    @PatchMapping("/api/notifications/{id}/read")
    public NotificationResponse markRead(@PathVariable Long id) {
        return NotificationResponse.from(notificationService.markRead(id));
    }

    @GetMapping("/api/incidents/{id}/notifications")
    public List<NotificationResponse> forIncident(@PathVariable Long id) {
        return notificationService.forIncident(id).stream().map(NotificationResponse::from).toList();
    }
}
