package com.itsm.state;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.itsm.enums.IncidentStatus;

/** Registry of the (stateless, shared) state objects, one per status. */
public final class IncidentStates {

    private static final Map<IncidentStatus, IncidentState> STATES = new EnumMap<>(IncidentStatus.class);

    static {
        List<IncidentState> all = List.of(
                new NewState(), new ClassifiedState(), new QueuedState(), new AssignedState(),
                new AcknowledgedState(), new InProgressState(), new ResolvedState(),
                new VerifiedState(), new ClosedState());
        for (IncidentState state : all) {
            STATES.put(state.status(), state);
        }
        if (STATES.size() != IncidentStatus.values().length) {
            throw new IllegalStateException("Every IncidentStatus needs a state class");
        }
    }

    private IncidentStates() {
    }

    public static IncidentState of(IncidentStatus status) {
        return STATES.get(status);
    }

    /** Statuses that count as a technician's open work. */
    public static Set<IncidentStatus> activeWorkStatuses() {
        return STATES.values().stream()
                .filter(IncidentState::isActiveWork)
                .map(IncidentState::status)
                .collect(Collectors.toUnmodifiableSet());
    }

    /** Statuses where the incident is no longer open. */
    public static Set<IncidentStatus> closedStatuses() {
        return Set.of(IncidentStatus.RESOLVED, IncidentStatus.VERIFIED, IncidentStatus.CLOSED);
    }
}
