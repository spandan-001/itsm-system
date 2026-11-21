package com.itsm.service.notification;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.itsm.event.DuplicateLinkEvent;
import com.itsm.event.IncidentAssignedEvent;
import com.itsm.event.IncidentCreatedEvent;
import com.itsm.event.IncidentEvent;
import com.itsm.event.IncidentStatusChangedEvent;
import com.itsm.event.SlaBreachedEvent;
import com.itsm.model.Incident;
import com.itsm.repository.IncidentRepository;

/**
 * Observer that decides who hears about each incident event. It runs only
 * after the incident change commits, so nobody is told about a change that was
 * rolled back. Each message is sent in its own transaction, and a failure is
 * logged without affecting the incident.
 */
@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationService notificationService;
    private final IncidentRepository incidentRepository;

    public NotificationEventListener(NotificationService notificationService,
                                     IncidentRepository incidentRepository) {
        this.notificationService = notificationService;
        this.incidentRepository = incidentRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Order(3)
    public void onIncidentEvent(IncidentEvent event) {
        Incident incident = event.getIncident();
        String ref = "Incident #" + incident.getId();
        Recipient reporter = Recipient.of(incident.getReporter());
        Recipient technician = incident.getAssignedTechnician() == null
                ? null : Recipient.of(incident.getAssignedTechnician());

        List<Message> messages = new ArrayList<>();
        switch (event) {
            case IncidentCreatedEvent created -> messages.add(new Message(reporter,
                    ref + " received",
                    "We logged \"" + incident.getTitle() + "\" with priority " + incident.getPriority()
                            + ". Target resolution: " + incident.getResolutionDueAt() + "."));
            case IncidentAssignedEvent assigned -> {
                messages.add(new Message(Recipient.of(assigned.getTechnician()),
                        ref + " assigned to you",
                        "\"" + incident.getTitle() + "\" (" + incident.getPriority()
                                + ") needs a response by " + incident.getResponseDueAt() + "."));
                messages.add(new Message(reporter,
                        ref + " assigned",
                        assigned.getTechnician().getName() + " is now working on your incident."));
            }
            case IncidentStatusChangedEvent changed -> {
                String text = "\"" + incident.getTitle() + "\" moved from " + changed.getFromStatus()
                        + " to " + changed.getToStatus() + ".";
                messages.add(new Message(reporter, ref + " is now " + changed.getToStatus(), text));
                // People whose reports were merged into this one follow its progress too.
                for (Incident duplicate : incidentRepository.findByDuplicateOf_Id(incident.getId())) {
                    messages.add(new Message(Recipient.of(duplicate.getReporter()),
                            "Incident #" + duplicate.getId() + " (linked to #" + incident.getId() + ") is now "
                                    + changed.getToStatus(), text));
                }
            }
            case DuplicateLinkEvent link -> messages.add(new Message(reporter,
                    link.isLinked() ? ref + " linked to an existing incident" : ref + " reopened as a separate incident",
                    link.isLinked()
                            ? "Your report matches incident #" + link.getOriginal().getId() + " (\""
                                    + link.getOriginal().getTitle() + "\"), which is already being handled."
                                    + " You will be updated as it progresses."
                            : "Your report is now tracked on its own with priority " + incident.getPriority() + "."));
            case SlaBreachedEvent breached -> {
                String text = breached.getSlaType() + " SLA for \"" + incident.getTitle() + "\" ("
                        + incident.getPriority() + ") was breached. Deadline: " + breached.getDeadline() + ".";
                messages.add(new Message(reporter, ref + " SLA breached", text));
                if (technician != null) {
                    messages.add(new Message(technician, ref + " SLA breached - action needed", text));
                }
            }
        }

        for (Message m : messages) {
            try {
                notificationService.notify(m.to(), m.subject(), m.body(), incident.getId());
            } catch (RuntimeException ex) {
                log.warn("Could not notify {} about {}: {}", m.to().email(), ref, ex.getMessage());
            }
        }
    }

    private record Message(Recipient to, String subject, String body) {
    }
}
