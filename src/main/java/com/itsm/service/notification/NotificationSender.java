package com.itsm.service.notification;

import com.itsm.enums.NotificationChannel;
import com.itsm.model.Notification;

/** One delivery channel. Add a channel by adding another implementation bean. */
public interface NotificationSender {

    NotificationChannel channel();

    Notification send(Recipient recipient, String subject, String message, Long incidentId);
}
