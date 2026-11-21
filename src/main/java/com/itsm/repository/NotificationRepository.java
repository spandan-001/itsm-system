package com.itsm.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.itsm.enums.NotificationChannel;
import com.itsm.model.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientEmailIgnoreCaseOrderByCreatedAtDescIdDesc(String recipientEmail);

    List<Notification> findByIncidentIdOrderByCreatedAtAscIdAsc(Long incidentId);

    long countByRecipientEmailIgnoreCaseAndChannelAndReadFalse(String recipientEmail, NotificationChannel channel);
}
