package com.itsm.strategy.priority;

import java.util.List;

import com.itsm.enums.Impact;
import com.itsm.enums.Priority;
import com.itsm.enums.Urgency;
import com.itsm.model.ITService;
import com.itsm.strategy.NamedStrategy;

/** Strategy pattern: one way of turning impact, urgency and services into P1–P4. */
public interface PriorityStrategy extends NamedStrategy {

    Priority calculate(Impact impact, Urgency urgency, List<ITService> affectedServices);

    /** 1 for LOW, 2 for MEDIUM, 3 for HIGH: all three enums share this scale. */
    static int level(Enum<?> value) {
        return value.ordinal() + 1;
    }

    /** Highest business criticality (1–3) among the services; 1 if there are none. */
    static int highestCriticality(List<ITService> services) {
        return services == null ? 1 : services.stream()
                .mapToInt(s -> level(s.getCriticality()))
                .max()
                .orElse(1);
    }
}
