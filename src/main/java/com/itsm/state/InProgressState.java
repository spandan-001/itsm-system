package com.itsm.state;

import com.itsm.enums.IncidentStatus;

final class InProgressState extends AbstractIncidentState {
    InProgressState() {
        super(IncidentStatus.IN_PROGRESS, "Technician is working on a fix", IncidentStatus.RESOLVED);
    }

    @Override
    public boolean isActiveWork() {
        return true;
    }
}
