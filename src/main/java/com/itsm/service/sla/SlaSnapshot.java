package com.itsm.service.sla;

import java.time.LocalDateTime;

import com.itsm.enums.IncidentStatus;
import com.itsm.enums.Priority;
import com.itsm.enums.SlaState;

/** Point-in-time view of an incident's SLA, for APIs and dashboards. */
public record SlaSnapshot(
        Long incidentId,
        String title,
        Priority priority,
        IncidentStatus status,
        LocalDateTime responseDueAt,
        LocalDateTime resolutionDueAt,
        LocalDateTime respondedAt,
        LocalDateTime resolvedAt,
        SlaState responseState,
        SlaState resolutionState,
        Long minutesUntilResolutionDue) {
}
