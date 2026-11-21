package com.itsm.strategy.assignment;

import org.springframework.stereotype.Component;

import com.itsm.model.Incident;

/**
 * Smart routing (intelligent feature): weighs skill match (50), spare capacity
 * (30) and track record on this category (20). Falls back to an unskilled
 * technician rather than queueing, except for P1 incidents, which wait for
 * a skilled one.
 */
@Component
public class SmartAssignmentStrategy extends AbstractAssignmentStrategy {

    static final int EXPERIENCE_FOR_FULL_SCORE = 5;

    @Override
    public String name() {
        return "smart";
    }

    @Override
    public String description() {
        return "Skill, workload and past resolutions combined";
    }

    @Override
    protected boolean isEligible(Incident incident, TechnicianCandidate candidate) {
        return incident.getPriority() != com.itsm.enums.Priority.P1 || skillMatch(incident, candidate);
    }

    @Override
    protected double score(Incident incident, TechnicianCandidate candidate) {
        double skill = skillMatch(incident, candidate) ? 1 : 0;
        double experience = Math.min(1.0, candidate.resolvedInCategory() / (double) EXPERIENCE_FOR_FULL_SCORE);
        return 50 * skill + 30 * candidate.spareCapacity() + 20 * experience;
    }
}
