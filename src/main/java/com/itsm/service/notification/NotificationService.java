package com.itsm.service.notification;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.itsm.enums.NotificationChannel;
import com.itsm.exception.ResourceNotFoundException;
import com.itsm.model.Notification;
import com.itsm.repository.NotificationRepository;

@Service
public class NotificationService {

    private final List<NotificationSender> senders;
    private final NotificationRepository notificationRepository;

    /** Spring injects every NotificationSender bean, so new channels need no change here. */
    public NotificationService(List<NotificationSender> senders,
                               NotificationRepository notificationRepository) {
        this.senders = List.copyOf(senders);
        this.notificationRepository = notificationRepository;
    }

    /** Sends the message over every channel, in its own transaction. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Notification> notify(Recipient recipient, String subject, String message, Long incidentId) {
        List<Notification> sent = new ArrayList<>();
        for (NotificationSender sender : senders) {
            sent.add(sender.send(recipient, subject, message, incidentId));
        }
        return sent;
    }

    @Transactional(readOnly = true)
    public List<Notification> forRecipient(String email) {
        return notificationRepository.findByRecipientEmailIgnoreCaseOrderByCreatedAtDescIdDesc(email);
    }

    @Transactional(readOnly = true)
    public List<Notification> forIncident(Long incidentId) {
        return notificationRepository.findByIncidentIdOrderByCreatedAtAscIdAsc(incidentId);
    }

    @Transactional(readOnly = true)
    public long unreadCount(String email) {
        return notificationRepository.countByRecipientEmailIgnoreCaseAndChannelAndReadFalse(
                email, NotificationChannel.IN_APP);
    }

    @Transactional
    public Notification markRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", id));
        notification.markRead();
        return notification;
    }
}
