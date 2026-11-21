package com.itsm.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import com.itsm.enums.NotificationChannel;

/** A notification delivered to one recipient over one channel. */
@Entity
@Table(name = "notifications", indexes = @Index(name = "idx_notifications_recipient", columnList = "recipientEmail"))
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String recipientName;

    @Column(nullable = false)
    private String recipientEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, length = 2000)
    private String message;

    private Long incidentId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    protected Notification() {
        // Required by JPA
    }

    public Notification(String recipientName, String recipientEmail, NotificationChannel channel,
                        String subject, String message, Long incidentId) {
        this.recipientName = recipientName;
        this.recipientEmail = recipientEmail;
        this.channel = channel;
        this.subject = subject;
        this.message = message;
        this.incidentId = incidentId;
        this.createdAt = LocalDateTime.now();
        this.read = false;
    }

    public void markRead() {
        this.read = true;
    }

    public Long getId() {
        return id;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public String getSubject() {
        return subject;
    }

    public String getMessage() {
        return message;
    }

    public Long getIncidentId() {
        return incidentId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isRead() {
        return read;
    }
}
