package com.itsm.state;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.itsm.TestData;
import com.itsm.enums.IncidentStatus;
import com.itsm.enums.Priority;
import com.itsm.exception.InvalidStateTransitionException;
import com.itsm.model.Incident;
import com.itsm.model.Technician;

class IncidentStateTest {

    private final Technician tech = new Technician("Ravi", "ravi@example.test", "NETWORK", true);

    @Test
    void everyStatusHasAStateObject() {
        for (IncidentStatus status : IncidentStatus.values()) {
            assertThat(IncidentStates.of(status).status()).isEqualTo(status);
        }
        assertThat(IncidentStates.of(IncidentStatus.CLOSED).isFinal()).isTrue();
    }

    @Test
    void happyPathFollowsTheLifecycle() {
        Incident incident = TestData.incident(Priority.P2);
        incident.transitionTo(IncidentStatus.CLASSIFIED);
        incident.assignTo(tech);
        for (IncidentStatus next : new IncidentStatus[] {IncidentStatus.ACKNOWLEDGED, IncidentStatus.IN_PROGRESS,
                IncidentStatus.RESOLVED, IncidentStatus.VERIFIED, IncidentStatus.CLOSED}) {
            incident.transitionTo(next);
        }
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.CLOSED);
        assertThat(incident.getAllowedTransitions()).isEmpty();
    }

    @Test
    void skippingStepsIsRejectedWithAllowedMoves() {
        Incident incident = TestData.incident(Priority.P2);

        assertThatThrownBy(() -> incident.transitionTo(IncidentStatus.CLOSED))
                .isInstanceOfSatisfying(InvalidStateTransitionException.class, ex -> {
                    assertThat(ex.getFrom()).isEqualTo(IncidentStatus.NEW);
                    assertThat(ex.getTo()).isEqualTo(IncidentStatus.CLOSED);
                    assertThat(ex.getAllowed()).containsExactly(IncidentStatus.CLASSIFIED);
                });
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.NEW);
    }

    @Test
    void assignedRequiresATechnician() {
        Incident incident = TestData.incident(Priority.P2);
        incident.transitionTo(IncidentStatus.CLASSIFIED);

        assertThatThrownBy(() -> incident.transitionTo(IncidentStatus.ASSIGNED))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("technician must be assigned");
    }

    @Test
    void failedAssignmentLeavesTechnicianUnchanged() {
        Incident incident = TestData.incident(Priority.P2); // still NEW, cannot be assigned

        assertThatThrownBy(() -> incident.assignTo(tech)).isInstanceOf(InvalidStateTransitionException.class);
        assertThat(incident.getAssignedTechnician()).isNull();
    }

    @Test
    void queuedAndReopenPathsAreAllowed() {
        assertThat(IncidentStates.of(IncidentStatus.CLASSIFIED).canTransitionTo(IncidentStatus.QUEUED)).isTrue();
        assertThat(IncidentStates.of(IncidentStatus.QUEUED).canTransitionTo(IncidentStatus.ASSIGNED)).isTrue();
        assertThat(IncidentStates.of(IncidentStatus.RESOLVED).canTransitionTo(IncidentStatus.IN_PROGRESS)).isTrue();
        assertThat(IncidentStates.activeWorkStatuses()).containsExactlyInAnyOrder(
                IncidentStatus.ASSIGNED, IncidentStatus.ACKNOWLEDGED, IncidentStatus.IN_PROGRESS);
    }
}
