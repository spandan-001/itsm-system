package com.itsm.enums;

/** The two SLA clocks tracked for every incident. */
public enum SlaType {
    /** Time until a technician acknowledges the incident. */
    RESPONSE,
    /** Time until the incident is resolved. */
    RESOLUTION
}
