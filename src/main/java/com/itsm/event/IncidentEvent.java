package com.itsm.event;

import java.time.LocalDateTime;

import com.itsm.enums.AuditAction;
import com.itsm.enums.IncidentStatus;
import com.itsm.model.Incident;

/**
 * Base type for everything that happens to an incident (Observer pattern).
 *
 * <p>{@code IncidentService} publishes these through Spring's
 * {@code ApplicationEventPublisher}. Observers (audit, notifications, SLA)
 * subscribe with {@code @EventListener} and never call each other. The class
 * is sealed so observers can switch over every event type exhaustively.
 */
public abstract sealed class IncidentEvent
        permits IncidentCreatedEvent, IncidentAssignedEvent,
                IncidentStatusChangedEvent, SlaBreachedEvent,
                DuplicateLinkEvent, IncidentNoteAddedEvent, SlaWarningEvent, FeedbackReceivedEvent,
                IncidentEscalatedEvent {

    public static final String SYSTEM_ACTOR = "system";

    private final Incident incident;
    private final String actor;
    private final LocalDateTime occurredAt;

    protected IncidentEvent(Incident incident, String actor) {
        if (incident == null) {
            throw new IllegalArgumentException("An incident event needs an incident");
        }
        this.incident = incident;
        this.actor = (actor == null || actor.isBlank()) ? SYSTEM_ACTOR : actor;
        this.occurredAt = LocalDateTime.now();
    }

    public Incident getIncident() {
        return incident;
    }

    public String getActor() {
        return actor;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    /** Which audit action this event records. */
    public abstract AuditAction getAuditAction();

    /** Human-readable summary, used for the audit trail. */
    public abstract String describe();

    /** Status before the event, or null when the status did not change. */
    public IncidentStatus getFromStatus() {
        return null;
    }

    /** Status after the event, or null when the status did not change. */
    public IncidentStatus getToStatus() {
        return null;
    }
}
