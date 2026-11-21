package com.itsm.service.knowledge;

import com.itsm.enums.IncidentCategory;
import com.itsm.enums.IncidentStatus;

/** A fix that worked for a similar past incident. */
public record ResolutionSuggestion(
        Long incidentId,
        String title,
        IncidentCategory category,
        IncidentStatus status,
        String resolution,
        String resolvedBy,
        Integer feedbackRating,
        boolean confirmed,
        double score) {
}
