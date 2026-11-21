package com.itsm.dto;

import java.time.LocalDateTime;

import com.itsm.enums.AuditAction;
import com.itsm.enums.IncidentStatus;
import com.itsm.model.AuditLog;

public record AuditLogResponse(
        Long id,
        Long incidentId,
        AuditAction action,
        IncidentStatus fromStatus,
        IncidentStatus toStatus,
        String details,
        String actor,
        LocalDateTime occurredAt) {

    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(log.getId(), log.getIncident().getId(), log.getAction(),
                log.getFromStatus(), log.getToStatus(), log.getDetails(), log.getActor(), log.getOccurredAt());
    }
}
