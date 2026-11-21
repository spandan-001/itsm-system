package com.itsm.strategy.assignment;

import java.util.Set;

import com.itsm.enums.IncidentCategory;

/** A ranked suggestion, with the reasoning, for the UI. */
public record TechnicianRecommendation(
        Long technicianId,
        String name,
        Set<IncidentCategory> skills,
        boolean skillMatch,
        int activeIncidents,
        int resolvedInCategory,
        double score,
        String reason) {

    static TechnicianRecommendation of(TechnicianCandidate c, boolean skillMatch, double score, String strategy) {
        String reason = (skillMatch ? "Has the required skill" : "No matching skill")
                + ", " + c.activeIncidents() + " open incident(s)"
                + ", " + c.resolvedInCategory() + " similar incident(s) resolved"
                + " [" + strategy + "]";
        return new TechnicianRecommendation(c.technician().getId(), c.technician().getName(),
                c.technician().getSkills(), skillMatch, c.activeIncidents(), c.resolvedInCategory(),
                Math.round(score * 10) / 10.0, reason);
    }
}
