package com.itsm.strategy.priority;

import java.util.List;

import org.springframework.stereotype.Component;

import com.itsm.enums.Impact;
import com.itsm.enums.Priority;
import com.itsm.enums.Urgency;
import com.itsm.model.ITService;

/**
 * impact × urgency × highest service criticality, each scored 1–3, then ×1,
 * ×1.5 or ×2 when fewer than 10, 10-99 or 100+ people are affected:
 * 18+ is P1, 9+ is P2, 4+ is P3, otherwise P4.
 */
@Component
public class WeightedScorePriorityStrategy implements PriorityStrategy {

    @Override
    public String name() {
        return "weighted";
    }

    @Override
    public String description() {
        return "Impact x urgency x business criticality x affected users";
    }

    @Override
    public Priority calculate(PriorityFactors factors) {
        double score = PriorityStrategy.level(factors.impact())
                * PriorityStrategy.level(factors.urgency())
                * PriorityStrategy.highestCriticality(factors.affectedServices())
                * (1 + 0.5 * factors.userScale());
        if (score >= 18) return Priority.P1;
        if (score >= 9) return Priority.P2;
        if (score >= 4) return Priority.P3;
        return Priority.P4;
    }
}
