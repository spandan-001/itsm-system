package com.itsm.state;

import com.itsm.enums.IncidentStatus;
import com.itsm.model.Incident;

/** A fix is in place. The reporter verifies it, or it is reopened. */
final class ResolvedState extends AbstractIncidentState {
    ResolvedState() {
        super(IncidentStatus.RESOLVED, "Fix applied, awaiting verification",
                IncidentStatus.VERIFIED, IncidentStatus.IN_PROGRESS);
    }

    @Override
    public void checkEntry(Incident incident, IncidentStatus from) {
        String summary = incident.getResolutionSummary();
        if (summary == null || summary.isBlank()) {
            throw refuse(from, "a resolution summary is required");
        }
    }
}
