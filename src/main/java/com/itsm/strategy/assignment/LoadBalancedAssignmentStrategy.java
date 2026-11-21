package com.itsm.strategy.assignment;

import org.springframework.stereotype.Component;

import com.itsm.model.Incident;

/** Whoever has the fewest open incidents; skill only breaks ties. */
@Component
public class LoadBalancedAssignmentStrategy extends AbstractAssignmentStrategy {

    @Override
    public String name() {
        return "load";
    }

    @Override
    public String description() {
        return "Least busy technician, skill breaks ties";
    }

    @Override
    protected double score(Incident incident, TechnicianCandidate candidate) {
        return 100 * candidate.spareCapacity() + (skillMatch(incident, candidate) ? 1 : 0);
    }
}
