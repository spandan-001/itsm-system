package com.itsm.event;

import java.time.LocalDateTime;

import com.itsm.enums.AuditAction;
import com.itsm.enums.SlaType;
import com.itsm.model.Incident;

public final class SlaBreachedEvent extends IncidentEvent {

    private final SlaType slaType;
    private final LocalDateTime deadline;

    public SlaBreachedEvent(Incident incident, SlaType slaType, LocalDateTime deadline) {
        super(incident, SYSTEM_ACTOR);
        this.slaType = slaType;
        this.deadline = deadline;
    }

    public SlaType getSlaType() {
        return slaType;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    @Override
    public AuditAction getAuditAction() {
        return AuditAction.SLA_BREACHED;
    }

    @Override
    public String describe() {
        return slaType + " SLA breached for priority " + getIncident().getPriority()
                + " (deadline was " + deadline + ")";
    }
}
