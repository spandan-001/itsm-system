package com.itsm.event;

import com.itsm.enums.AuditAction;
import com.itsm.enums.EscalationLevel;
import com.itsm.model.Incident;
import com.itsm.service.notification.Recipient;

/** A critical incident was not acknowledged in time and moved up the escalation chain. */
public final class IncidentEscalatedEvent extends IncidentEvent {

    private final EscalationLevel from;
    private final EscalationLevel to;
    private final Recipient contact;

    public IncidentEscalatedEvent(Incident incident, EscalationLevel from, EscalationLevel to, Recipient contact) {
        super(incident, SYSTEM_ACTOR);
        this.from = from;
        this.to = to;
        this.contact = contact;
    }

    public EscalationLevel getFrom() {
        return from;
    }

    public EscalationLevel getTo() {
        return to;
    }

    public Recipient getContact() {
        return contact;
    }

    @Override
    public AuditAction getAuditAction() {
        return AuditAction.ESCALATED;
    }

    @Override
    public String describe() {
        return "Not acknowledged in time: escalated from " + from + " to " + to + " (" + contact.name() + ")";
    }
}
