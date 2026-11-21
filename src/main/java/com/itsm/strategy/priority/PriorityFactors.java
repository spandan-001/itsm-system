package com.itsm.strategy.priority;

import java.util.List;

import com.itsm.enums.Impact;
import com.itsm.enums.Urgency;
import com.itsm.model.ITService;

/** Everything a priority strategy may weigh. */
public record PriorityFactors(Impact impact, Urgency urgency, List<ITService> affectedServices, int affectedUsers) {

    public PriorityFactors {
        if (affectedUsers < 1) {
            throw new IllegalArgumentException("At least one user must be affected");
        }
        affectedServices = affectedServices == null ? List.of() : List.copyOf(affectedServices);
    }

    /** 0 for fewer than 10 people, 1 for 10-99, 2 for 100 or more. */
    public int userScale() {
        return affectedUsers >= 100 ? 2 : affectedUsers >= 10 ? 1 : 0;
    }
}
