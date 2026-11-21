package com.itsm.service.analytics;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.itsm.enums.IncidentCategory;
import com.itsm.enums.IncidentStatus;
import com.itsm.enums.Priority;

/** Everything the operational dashboard needs, in one response. */
public record AnalyticsSummary(
        long totalIncidents,
        long openIncidents,
        long queuedIncidents,
        long duplicateIncidents,
        long slaBreachedIncidents,
        Double slaCompliancePercent,
        Double averageResolutionMinutes,
        Map<IncidentStatus, Long> byStatus,
        Map<Priority, Long> byPriority,
        Map<IncidentCategory, Long> byCategory,
        Map<Priority, Double> averageResolutionMinutesByPriority,
        Map<LocalDate, Long> createdPerDay,
        List<TechnicianStats> technicians) {

    public record TechnicianStats(Long id, String name, boolean available, Set<IncidentCategory> skills,
                                  long activeIncidents, long resolvedIncidents) {
    }
}
