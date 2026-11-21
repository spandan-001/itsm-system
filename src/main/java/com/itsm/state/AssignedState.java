package com.itsm.state;

import com.itsm.enums.IncidentStatus;
import com.itsm.model.Incident;

final class AssignedState extends AbstractIncidentState {
    AssignedState() {
        super(IncidentStatus.ASSIGNED, "Assigned, awaiting technician acknowledgement", IncidentStatus.ACKNOWLEDGED);
    }

    @Override
    public void checkEntry(Incident incident, IncidentStatus from) {
        if (incident.getAssignedTechnician() == null) {
            throw refuse(from, "a technician must be assigned (use the assignment endpoint)");
        }
    }

    @Override
    public boolean isActiveWork() {
        return true;
    }
}
