package com.itsm.sla;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.itsm.enums.Priority;
import com.itsm.model.SlaPolicy;

class SlaPolicyTest {

    @Test
    void defaultTargetsGetLongerAsPriorityDrops() {
        Duration previous = Duration.ZERO;
        for (Priority priority : Priority.values()) {
            SlaPolicy policy = SlaPolicy.defaultFor(priority);
            assertThat(policy.resolutionTarget()).isGreaterThan(previous);
            assertThat(policy.resolutionTarget()).isGreaterThanOrEqualTo(policy.responseTarget());
            previous = policy.resolutionTarget();
        }
        assertThat(SlaPolicy.defaultFor(Priority.P1).responseTarget()).isEqualTo(Duration.ofMinutes(15));
        assertThat(SlaPolicy.defaultFor(Priority.P1).resolutionTarget()).isEqualTo(Duration.ofHours(4));
    }

    @Test
    void rejectsNonPositiveOrInconsistentTargets() {
        assertThatThrownBy(() -> new SlaPolicy(Priority.P2, 0, 60))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SlaPolicy(Priority.P2, 120, 60))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Resolution target");
    }
}
