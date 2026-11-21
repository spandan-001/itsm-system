package com.itsm.service.duplicate;

import com.itsm.enums.IncidentStatus;
import com.itsm.enums.Priority;
import com.itsm.model.Incident;

/** An existing incident that resembles a new report. */
public record DuplicateMatch(Long incidentId, String title, IncidentStatus status, Priority priority,
                             double score, boolean likelyDuplicate) {

    static DuplicateMatch of(Incident incident, double score, double threshold) {
        return new DuplicateMatch(incident.getId(), incident.getTitle(), incident.getStatus(),
                incident.getPriority(), Math.round(score * 1000) / 1000.0, score >= threshold);
    }
}
