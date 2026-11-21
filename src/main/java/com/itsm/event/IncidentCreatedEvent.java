package com.itsm.event;

import com.itsm.enums.AuditAction;
import com.itsm.enums.IncidentStatus;
import com.itsm.model.Incident;

public final class IncidentCreatedEvent extends IncidentEvent {

    public IncidentCreatedEvent(Incident incident, String actor) {
        super(incident, actor);
    }

    @Override
    public AuditAction getAuditAction() {
        return AuditAction.CREATED;
    }

    @Override
    public IncidentStatus getToStatus() {
        return getIncident().getStatus();
    }

    @Override
    public String describe() {
        Incident incident = getIncident();
        return "Incident reported by " + incident.getReporter().getName()
                + " with priority " + incident.getPriority()
                + " (impact " + incident.getImpact() + ", urgency " + incident.getUrgency() + ")";
    }
}
