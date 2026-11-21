package com.itsm.event;

import java.time.LocalDateTime;

import com.itsm.enums.AuditAction;
import com.itsm.enums.SlaType;
import com.itsm.model.Incident;

/** An SLA clock has used most of its window and is at risk of breaching. */
public final class SlaWarningEvent extends IncidentEvent {

    private final SlaType slaType;
    private final LocalDateTime deadline;

    public SlaWarningEvent(Incident incident, SlaType slaType, LocalDateTime deadline) {
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
        return AuditAction.SLA_WARNING;
    }

    @Override
    public String describe() {
        return slaType + " SLA at risk for priority " + getIncident().getPriority() + " (deadline " + deadline + ")";
    }
}
