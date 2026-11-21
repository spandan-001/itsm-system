package com.itsm.enums;

/**
 * Escalation chain for critical incidents nobody has acknowledged:
 * Technician, then Team Lead, System Administrator and IT Manager. Each level
 * starts once this many response windows have passed without acknowledgement.
 */
public enum EscalationLevel {
    NONE(0),
    TEAM_LEAD(1.0),
    SYSTEM_ADMINISTRATOR(1.5),
    IT_MANAGER(2.0);

    private final double responseWindows;

    EscalationLevel(double responseWindows) {
        this.responseWindows = responseWindows;
    }

    public double responseWindows() {
        return responseWindows;
    }

    /** The highest level reached after this many response windows. */
    public static EscalationLevel forElapsed(double windows) {
        EscalationLevel reached = NONE;
        for (EscalationLevel level : values()) {
            if (level != NONE && windows >= level.responseWindows) {
                reached = level;
            }
        }
        return reached;
    }
}
