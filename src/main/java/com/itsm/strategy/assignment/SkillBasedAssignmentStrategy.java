package com.itsm.strategy.assignment;

import org.springframework.stereotype.Component;

import com.itsm.model.Incident;

/** Only technicians with the incident's category as a skill; least busy first. */
@Component
public class SkillBasedAssignmentStrategy extends AbstractAssignmentStrategy {

    @Override
    public String name() {
        return "skill";
    }

    @Override
    public String description() {
        return "Skilled technicians only, least busy first";
    }

    @Override
    protected boolean isEligible(Incident incident, TechnicianCandidate candidate) {
        return skillMatch(incident, candidate);
    }

    @Override
    protected double score(Incident incident, TechnicianCandidate candidate) {
        return 100 * candidate.spareCapacity();
    }
}
