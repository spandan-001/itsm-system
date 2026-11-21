package com.itsm.enums;

/**
 * Incident lifecycle. Which moves are legal is defined by the State pattern
 * classes in {@code com.itsm.state}, not here.
 */
public enum IncidentStatus {
    NEW,
    CLASSIFIED,
    /** Classified, but no suitable technician was free. Waits for routing. */
    QUEUED,
    ASSIGNED,
    ACKNOWLEDGED,
    IN_PROGRESS,
    RESOLVED,
    VERIFIED,
    CLOSED
}
