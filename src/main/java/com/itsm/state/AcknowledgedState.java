package com.itsm.state;

import com.itsm.enums.IncidentStatus;

final class AcknowledgedState extends AbstractIncidentState {
    AcknowledgedState() {
        super(IncidentStatus.ACKNOWLEDGED, "Technician has acknowledged the incident", IncidentStatus.IN_PROGRESS);
    }

    @Override
    public boolean isActiveWork() {
        return true;
    }
}
