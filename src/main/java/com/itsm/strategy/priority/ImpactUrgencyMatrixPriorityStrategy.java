package com.itsm.strategy.priority;

import java.util.List;

import org.springframework.stereotype.Component;

import com.itsm.enums.Impact;
import com.itsm.enums.Priority;
import com.itsm.enums.Urgency;
import com.itsm.model.ITService;

/**
 * The classic ITIL impact/urgency matrix, raised one level when a
 * business-critical (HIGH) service is affected, and one more level each at
 * 10 and 100 affected people.
 *
 * <pre>
 *              urgency LOW  MEDIUM  HIGH
 * impact HIGH          P3   P2      P1
 *        MEDIUM        P4   P3      P2
 *        LOW           P4   P4      P3
 * </pre>
 */
@Component
public class ImpactUrgencyMatrixPriorityStrategy implements PriorityStrategy {

    @Override
    public String name() {
        return "matrix";
    }

    @Override
    public String description() {
        return "ITIL impact/urgency matrix, escalated for critical services";
    }

    @Override
    public Priority calculate(PriorityFactors factors) {
        int sum = PriorityStrategy.level(factors.impact()) + PriorityStrategy.level(factors.urgency()); // 2..6
        int index = switch (sum) {
            case 6 -> 0;  // P1
            case 5 -> 1;  // P2
            case 4 -> 2;  // P3
            default -> 3; // P4
        };
        if (PriorityStrategy.highestCriticality(factors.affectedServices()) == 3) {
            index--;
        }
        index = Math.max(0, index - factors.userScale());
        return Priority.values()[index];
    }
}
