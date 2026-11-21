package com.itsm.dto;

import java.time.LocalDateTime;

import com.itsm.enums.NotificationChannel;
import com.itsm.model.Notification;

public record NotificationResponse(
        Long id,
        String recipientName,
        String recipientEmail,
        NotificationChannel channel,
        String subject,
        String message,
        Long incidentId,
        LocalDateTime createdAt,
        boolean read) {

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getRecipientName(), n.getRecipientEmail(), n.getChannel(),
                n.getSubject(), n.getMessage(), n.getIncidentId(), n.getCreatedAt(), n.isRead());
    }
}
