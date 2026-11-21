package com.itsm.exception;

import java.util.Set;

import com.itsm.enums.IncidentStatus;

/** Thrown when an incident is asked to make a move its current state forbids. */
public class InvalidStateTransitionException extends ItsmException {

    private final IncidentStatus from;
    private final IncidentStatus to;
    private final Set<IncidentStatus> allowed;

    public InvalidStateTransitionException(IncidentStatus from, IncidentStatus to, Set<IncidentStatus> allowed) {
        this(from, to, allowed, "Cannot move incident from " + from + " to " + to
                + (allowed.isEmpty() ? " (" + from + " is final)" : ". Allowed next: " + allowed));
    }

    public InvalidStateTransitionException(IncidentStatus from, IncidentStatus to,
                                           Set<IncidentStatus> allowed, String message) {
        super(message);
        this.from = from;
        this.to = to;
        this.allowed = Set.copyOf(allowed);
    }

    public IncidentStatus getFrom() {
        return from;
    }

    public IncidentStatus getTo() {
        return to;
    }

    public Set<IncidentStatus> getAllowed() {
        return allowed;
    }
}
