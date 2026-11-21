package com.itsm.service.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.itsm.enums.NotificationChannel;
import com.itsm.model.Notification;
import com.itsm.repository.NotificationRepository;

/**
 * Simulated email: writes the message to the application log (an "outbox")
 * and keeps a copy in the database. Swap {@link #deliver} for JavaMailSender
 * to send real mail.
 */
@Component
public class EmailNotificationSender extends AbstractNotificationSender {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationSender.class);

    public EmailNotificationSender(NotificationRepository notificationRepository) {
        super(notificationRepository);
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    protected void deliver(Notification notification) {
        log.info("[EMAIL] To: {} <{}> | Subject: {} | {}",
                notification.getRecipientName(), notification.getRecipientEmail(),
                notification.getSubject(), notification.getMessage());
    }
}
