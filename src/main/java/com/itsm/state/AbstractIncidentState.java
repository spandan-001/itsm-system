package com.itsm.state;

import java.util.EnumSet;
import java.util.Set;

import com.itsm.enums.IncidentStatus;
import com.itsm.exception.InvalidStateTransitionException;
import com.itsm.model.Incident;

/** Shared transition logic. Subclasses declare their successors and entry checks. */
abstract class AbstractIncidentState implements IncidentState {

    private final IncidentStatus status;
    private final Set<IncidentStatus> allowedNext;
    private final String description;

    protected AbstractIncidentState(IncidentStatus status, String description, IncidentStatus... allowedNext) {
        this.status = status;
        this.description = description;
        this.allowedNext = allowedNext.length == 0
                ? Set.of()
                : Set.copyOf(EnumSet.of(allowedNext[0], allowedNext));
    }

    @Override
    public final IncidentStatus status() {
        return status;
    }

    @Override
    public final Set<IncidentStatus> allowedNext() {
        return allowedNext;
    }

    @Override
    public final String description() {
        return description;
    }

    @Override
    public final void validateTransition(Incident incident, IncidentStatus target) {
        if (!canTransitionTo(target)) {
            throw new InvalidStateTransitionException(status, target, allowedNext);
        }
        IncidentStates.of(target).checkEntry(incident, status);
    }

    @Override
    public void checkEntry(Incident incident, IncidentStatus from) {
        // Most states have no entry precondition.
    }

    protected final InvalidStateTransitionException refuse(IncidentStatus from, String reason) {
        return new InvalidStateTransitionException(from, status,
                IncidentStates.of(from).allowedNext(),
                "Cannot move incident from " + from + " to " + status + ": " + reason);
    }
}
