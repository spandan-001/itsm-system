package com.itsm.state;

import java.util.Set;

import com.itsm.enums.IncidentStatus;
import com.itsm.model.Incident;

/**
 * State pattern: one object per lifecycle status. Each state knows which
 * statuses may follow it and what must be true to enter it, so the rules
 * live in one place instead of in switch statements across services.
 */
public interface IncidentState {

    IncidentStatus status();

    /** Statuses this state may move to. Empty for a final state. */
    Set<IncidentStatus> allowedNext();

    /** Short explanation for the UI, e.g. "Waiting for a technician". */
    String description();

    /** Throws {@code InvalidStateTransitionException} unless the move is legal. */
    void validateTransition(Incident incident, IncidentStatus target);

    /** Checks preconditions for entering this state. Throws if they fail. */
    void checkEntry(Incident incident, IncidentStatus from);

    default boolean canTransitionTo(IncidentStatus target) {
        return allowedNext().contains(target);
    }

    default boolean isFinal() {
        return allowedNext().isEmpty();
    }

    /** Whether the incident counts as open work (for workload and SLA). */
    default boolean isActiveWork() {
        return false;
    }
}
