package com.itsm.service.notification;

import org.springframework.stereotype.Component;

import com.itsm.enums.NotificationChannel;
import com.itsm.model.Notification;
import com.itsm.repository.NotificationRepository;

/** In-app inbox: the stored record is the delivery, shown in the user's dashboard. */
@Component
public class InAppNotificationSender extends AbstractNotificationSender {

    public InAppNotificationSender(NotificationRepository notificationRepository) {
        super(notificationRepository);
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.IN_APP;
    }

    @Override
    protected void deliver(Notification notification) {
        // Nothing further: the dashboard reads unread notifications from the database.
    }
}
