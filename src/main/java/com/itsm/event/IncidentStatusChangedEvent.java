package com.itsm.event;

import com.itsm.enums.AuditAction;
import com.itsm.enums.IncidentStatus;
import com.itsm.model.Incident;

public final class IncidentStatusChangedEvent extends IncidentEvent {

    private final IncidentStatus fromStatus;
    private final IncidentStatus toStatus;

    public IncidentStatusChangedEvent(Incident incident, IncidentStatus fromStatus,
                                      IncidentStatus toStatus, String actor) {
        super(incident, actor);
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
    }

    @Override
    public AuditAction getAuditAction() {
        return AuditAction.STATUS_CHANGED;
    }

    @Override
    public IncidentStatus getFromStatus() {
        return fromStatus;
    }

    @Override
    public IncidentStatus getToStatus() {
        return toStatus;
    }

    @Override
    public String describe() {
        return "Status changed from " + fromStatus + " to " + toStatus;
    }
}
