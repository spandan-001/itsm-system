package com.itsm.state;

import com.itsm.enums.IncidentStatus;

final class VerifiedState extends AbstractIncidentState {
    VerifiedState() {
        super(IncidentStatus.VERIFIED, "Fix confirmed, ready to close", IncidentStatus.CLOSED);
    }
}
