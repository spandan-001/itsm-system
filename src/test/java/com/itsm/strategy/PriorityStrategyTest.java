package com.itsm.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.itsm.enums.BusinessCriticality;
import com.itsm.enums.Impact;
import com.itsm.enums.Priority;
import com.itsm.enums.Urgency;
import com.itsm.model.ITService;
import com.itsm.strategy.priority.ImpactUrgencyMatrixPriorityStrategy;
import com.itsm.strategy.priority.PriorityCalculator;
import com.itsm.strategy.priority.PriorityStrategy;
import com.itsm.strategy.priority.WeightedScorePriorityStrategy;

class PriorityStrategyTest {

    private static List<ITService> service(BusinessCriticality criticality) {
        return List.of(new ITService("svc-" + criticality, "", criticality));
    }

    @Test
    void weightedScoreMultipliesImpactUrgencyAndCriticality() {
        PriorityStrategy weighted = new WeightedScorePriorityStrategy();
        assertThat(weighted.calculate(Impact.HIGH, Urgency.HIGH, service(BusinessCriticality.HIGH))).isEqualTo(Priority.P1); // 27
        assertThat(weighted.calculate(Impact.HIGH, Urgency.HIGH, service(BusinessCriticality.LOW))).isEqualTo(Priority.P2);  // 9
        assertThat(weighted.calculate(Impact.MEDIUM, Urgency.MEDIUM, service(BusinessCriticality.LOW))).isEqualTo(Priority.P3); // 4
        assertThat(weighted.calculate(Impact.LOW, Urgency.LOW, service(BusinessCriticality.HIGH))).isEqualTo(Priority.P4);  // 3
    }

    @Test
    void matrixEscalatesForCriticalServices() {
        PriorityStrategy matrix = new ImpactUrgencyMatrixPriorityStrategy();
        assertThat(matrix.calculate(Impact.HIGH, Urgency.MEDIUM, service(BusinessCriticality.LOW))).isEqualTo(Priority.P2);
        assertThat(matrix.calculate(Impact.HIGH, Urgency.MEDIUM, service(BusinessCriticality.HIGH))).isEqualTo(Priority.P1);
        assertThat(matrix.calculate(Impact.HIGH, Urgency.HIGH, service(BusinessCriticality.HIGH))).isEqualTo(Priority.P1);
        assertThat(matrix.calculate(Impact.LOW, Urgency.LOW, service(BusinessCriticality.MEDIUM))).isEqualTo(Priority.P4);
    }

    @Test
    void calculatorSwitchesStrategyAtRuntime() {
        PriorityCalculator calculator = new PriorityCalculator(
                List.of(new WeightedScorePriorityStrategy(), new ImpactUrgencyMatrixPriorityStrategy()), "weighted");
        List<ITService> lowService = service(BusinessCriticality.LOW);

        assertThat(calculator.calculate(Impact.HIGH, Urgency.MEDIUM, lowService)).isEqualTo(Priority.P3); // 6
        calculator.strategies().use("matrix");
        assertThat(calculator.calculate(Impact.HIGH, Urgency.MEDIUM, lowService)).isEqualTo(Priority.P2);
        assertThatThrownBy(() -> calculator.strategies().use("random"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Available");
    }
}
