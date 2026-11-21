package com.itsm.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.itsm.TestData;
import com.itsm.enums.IncidentCategory;
import com.itsm.enums.Priority;
import com.itsm.model.Incident;
import com.itsm.model.Technician;
import com.itsm.strategy.assignment.LoadBalancedAssignmentStrategy;
import com.itsm.strategy.assignment.SkillBasedAssignmentStrategy;
import com.itsm.strategy.assignment.SmartAssignmentStrategy;
import com.itsm.strategy.assignment.TechnicianCandidate;
import com.itsm.strategy.assignment.TechnicianRecommendation;

class AssignmentStrategyTest {

    private static TechnicianCandidate candidate(String name, IncidentCategory skill, int active, int resolved) {
        return new TechnicianCandidate(
                new Technician(name, name + "@example.test", skill.name(), true, EnumSet.of(skill)), active, resolved);
    }

    private final Incident networkP2 = TestData.incident(Priority.P2); // NETWORK category
    private final TechnicianCandidate busyNetwork = candidate("Ravi", IncidentCategory.NETWORK, 3, 0);
    private final TechnicianCandidate idleHardware = candidate("Meera", IncidentCategory.HARDWARE, 0, 0);

    private static List<String> names(List<TechnicianRecommendation> ranked) {
        return ranked.stream().map(TechnicianRecommendation::name).toList();
    }

    @Test
    void skillBasedOnlyConsidersSkilledTechnicians() {
        var ranked = new SkillBasedAssignmentStrategy().rank(networkP2, List.of(busyNetwork, idleHardware));
        assertThat(names(ranked)).containsExactly("Ravi");

        var none = new SkillBasedAssignmentStrategy().rank(networkP2, List.of(idleHardware));
        assertThat(none).isEmpty(); // would be queued
    }

    @Test
    void loadBalancedPicksTheLeastBusy() {
        var ranked = new LoadBalancedAssignmentStrategy().rank(networkP2, List.of(busyNetwork, idleHardware));
        assertThat(names(ranked)).containsExactly("Meera", "Ravi");
    }

    @Test
    void smartWeighsSkillWorkloadAndExperience() {
        TechnicianCandidate experienced = candidate("Arjun", IncidentCategory.NETWORK, 2, 5);
        var ranked = new SmartAssignmentStrategy().rank(networkP2, List.of(busyNetwork, idleHardware, experienced));

        assertThat(names(ranked)).containsExactly("Arjun", "Ravi", "Meera");
        assertThat(ranked.get(0).skillMatch()).isTrue();
        assertThat(ranked.get(0).reason()).contains("5 similar incident(s) resolved");
    }

    @Test
    void smartRequiresSkillForP1AndEveryoneRespectsCapacity() {
        Incident networkP1 = TestData.incident(Priority.P1);
        assertThat(new SmartAssignmentStrategy().rank(networkP1, List.of(idleHardware))).isEmpty();

        TechnicianCandidate full = candidate("Full", IncidentCategory.NETWORK, TechnicianCandidate.MAX_ACTIVE_INCIDENTS, 9);
        assertThat(new LoadBalancedAssignmentStrategy().rank(networkP2, List.of(full))).isEmpty();
    }
}
