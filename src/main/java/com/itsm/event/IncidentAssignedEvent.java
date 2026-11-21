package com.itsm.event;

import com.itsm.enums.AuditAction;
import com.itsm.enums.IncidentStatus;
import com.itsm.model.Incident;
import com.itsm.model.Technician;

public final class IncidentAssignedEvent extends IncidentEvent {

    private final IncidentStatus fromStatus;
    private final Technician technician;

    public IncidentAssignedEvent(Incident incident, IncidentStatus fromStatus,
                                 Technician technician, String actor) {
        super(incident, actor);
        this.fromStatus = fromStatus;
        this.technician = technician;
    }

    public Technician getTechnician() {
        return technician;
    }

    @Override
    public AuditAction getAuditAction() {
        return AuditAction.ASSIGNED;
    }

    @Override
    public IncidentStatus getFromStatus() {
        return fromStatus;
    }

    @Override
    public IncidentStatus getToStatus() {
        return IncidentStatus.ASSIGNED;
    }

    @Override
    public String describe() {
        return "Assigned to " + technician.getName() + " (" + technician.getDomain() + ")";
    }
}
