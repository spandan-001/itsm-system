package com.itsm.service.notification;

import com.itsm.model.Notification;
import com.itsm.repository.NotificationRepository;

/**
 * Template method: every channel stores a delivery record the same way, and
 * subclasses supply only the channel-specific {@link #deliver} step.
 */
public abstract class AbstractNotificationSender implements NotificationSender {

    private final NotificationRepository notificationRepository;

    protected AbstractNotificationSender(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public final Notification send(Recipient recipient, String subject, String message, Long incidentId) {
        Notification notification = new Notification(
                recipient.name(), recipient.email(), channel(), subject, message, incidentId);
        Notification saved = notificationRepository.save(notification);
        deliver(saved);
        return saved;
    }

    /** Channel-specific delivery after the record is stored. */
    protected abstract void deliver(Notification notification);
}
