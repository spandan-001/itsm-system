package com.itsm.strategy.assignment;

import com.itsm.model.Technician;

/**
 * A technician being considered for an incident, with live workload data.
 *
 * @param activeIncidents     open incidents currently assigned to them
 * @param resolvedInCategory  incidents of this category they have resolved before
 */
public record TechnicianCandidate(Technician technician, int activeIncidents, int resolvedInCategory) {

    /** Technicians at this many open incidents are not given more. */
    public static final int MAX_ACTIVE_INCIDENTS = 5;

    public boolean hasCapacity() {
        return technician.isAvailable() && activeIncidents < MAX_ACTIVE_INCIDENTS;
    }

    /** 1.0 when idle, 0.0 when full. */
    public double spareCapacity() {
        return Math.max(0, MAX_ACTIVE_INCIDENTS - activeIncidents) / (double) MAX_ACTIVE_INCIDENTS;
    }
}
