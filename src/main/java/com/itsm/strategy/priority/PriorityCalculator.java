package com.itsm.strategy.priority;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.itsm.enums.Impact;
import com.itsm.enums.Priority;
import com.itsm.enums.Urgency;
import com.itsm.model.ITService;
import com.itsm.strategy.StrategyRegistry;

/** Strategy context: delegates to whichever priority strategy is active. */
@Service
public class PriorityCalculator {

    private final StrategyRegistry<PriorityStrategy> registry;

    public PriorityCalculator(List<PriorityStrategy> strategies,
                              @Value("${itsm.priority.strategy:weighted}") String defaultStrategy) {
        this.registry = new StrategyRegistry<>("priority", strategies, defaultStrategy);
    }

    public Priority calculate(Impact impact, Urgency urgency, List<ITService> affectedServices) {
        return registry.active().calculate(impact, urgency, affectedServices);
    }

    public StrategyRegistry<PriorityStrategy> strategies() {
        return registry;
    }
}
