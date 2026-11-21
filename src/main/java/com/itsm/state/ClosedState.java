package com.itsm.state;

import com.itsm.enums.IncidentStatus;

final class ClosedState extends AbstractIncidentState {
    ClosedState() {
        super(IncidentStatus.CLOSED, "Closed");
    }
}
