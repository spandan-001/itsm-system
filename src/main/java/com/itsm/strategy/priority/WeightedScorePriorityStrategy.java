package com.itsm.strategy.priority;

import java.util.List;

import org.springframework.stereotype.Component;

import com.itsm.enums.Impact;
import com.itsm.enums.Priority;
import com.itsm.enums.Urgency;
import com.itsm.model.ITService;

/**
 * impact × urgency × highest service criticality, each scored 1–3, giving
 * 1–27: 18+ is P1, 9+ is P2, 4+ is P3, otherwise P4.
 */
@Component
public class WeightedScorePriorityStrategy implements PriorityStrategy {

    @Override
    public String name() {
        return "weighted";
    }

    @Override
    public String description() {
        return "Impact x urgency x business criticality score";
    }

    @Override
    public Priority calculate(Impact impact, Urgency urgency, List<ITService> affectedServices) {
        int score = PriorityStrategy.level(impact)
                * PriorityStrategy.level(urgency)
                * PriorityStrategy.highestCriticality(affectedServices);
        if (score >= 18) return Priority.P1;
        if (score >= 9) return Priority.P2;
        if (score >= 4) return Priority.P3;
        return Priority.P4;
    }
}
