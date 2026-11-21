package com.itsm.state;

import com.itsm.enums.IncidentStatus;

/** A fix is in place. The reporter verifies it, or it is reopened. */
final class ResolvedState extends AbstractIncidentState {
    ResolvedState() {
        super(IncidentStatus.RESOLVED, "Fix applied, awaiting verification",
                IncidentStatus.VERIFIED, IncidentStatus.IN_PROGRESS);
    }
}
