package com.itsm.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.itsm.service.AccountService;
import com.itsm.service.IncidentService;

import com.itsm.dto.NotificationResponse;
import com.itsm.service.notification.NotificationService;

/** Notification inbox for the linked ITSM account. */
@RestController
public class NotificationController {

    private final NotificationService notificationService;

    private final AccountService accounts;
    private final IncidentService incidents;

    public NotificationController(NotificationService notificationService, AccountService accounts,
                                  IncidentService incidents) {
        this.notificationService = notificationService;
        this.accounts = accounts;
        this.incidents = incidents;
    }

    @GetMapping("/api/notifications")
    public List<NotificationResponse> forRecipient() {
        return notificationService.forRecipient(accounts.requireCurrentAccount().email()).stream().map(NotificationResponse::from).toList();
    }

    @GetMapping("/api/notifications/unread-count")
    public Map<String, Long> unreadCount() {
        return Map.of("unread", notificationService.unreadCount(accounts.requireCurrentAccount().email()));
    }

    @PatchMapping("/api/notifications/{id}/read")
    public NotificationResponse markRead(@PathVariable Long id) {
        return NotificationResponse.from(notificationService.markRead(id, accounts.requireCurrentAccount().email()));
    }

    @GetMapping("/api/incidents/{id}/notifications")
    public List<NotificationResponse> forIncident(@PathVariable Long id) {
        var account = accounts.requireCurrentAccount();
        incidents.getIncident(id, account);
        return notificationService.forIncident(id).stream()
                .filter(notification -> account.email().equalsIgnoreCase(notification.getRecipientEmail()))
                .map(NotificationResponse::from).toList();
    }
}
