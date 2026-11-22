package com.itsm.service.search;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import com.itsm.enums.IncidentCategory;
import com.itsm.enums.IncidentStatus;
import com.itsm.enums.Priority;

/**
 * Optional search filters, bound from query parameters. Every null field is
 * ignored; the rest are combined with AND.
 */
public record IncidentSearchCriteria(
        List<IncidentStatus> status,
        List<Priority> priority,
        IncidentCategory category,
        Long technicianId,
        Long reporterId,
        Long serviceId,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdFrom,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdTo,
        String keyword,
        Boolean slaBreached,
        Boolean duplicate,
        Boolean unassigned,
        Long incidentId,
        Boolean awaitingInfo) {

    public static IncidentSearchCriteria none() {
        return new IncidentSearchCriteria(null, null, null, null, null, null, null, null, null, null, null, null,
                null, null);
    }
}
