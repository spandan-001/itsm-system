package com.itsm.state;

import com.itsm.enums.IncidentStatus;

final class QueuedState extends AbstractIncidentState {
    QueuedState() {
        super(IncidentStatus.QUEUED, "Waiting for a free technician", IncidentStatus.ASSIGNED);
    }
}
