package com.itsm.strategy.assignment;

import java.util.Comparator;
import java.util.List;

import com.itsm.model.Incident;

/**
 * Template method: filter eligible candidates, score each, sort best first.
 * Subclasses supply only {@link #isEligible} and {@link #score}.
 */
public abstract class AbstractAssignmentStrategy implements AssignmentStrategy {

    @Override
    public final List<TechnicianRecommendation> rank(Incident incident, List<TechnicianCandidate> candidates) {
        return candidates.stream()
                .filter(TechnicianCandidate::hasCapacity)
                .filter(c -> isEligible(incident, c))
                .map(c -> TechnicianRecommendation.of(c, skillMatch(incident, c), score(incident, c), name()))
                .sorted(Comparator.comparingDouble(TechnicianRecommendation::score).reversed()
                        .thenComparing(TechnicianRecommendation::technicianId,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    protected boolean isEligible(Incident incident, TechnicianCandidate candidate) {
        return true;
    }

    /** Higher is better. */
    protected abstract double score(Incident incident, TechnicianCandidate candidate);

    protected static boolean skillMatch(Incident incident, TechnicianCandidate candidate) {
        return candidate.technician().hasSkill(incident.getCategory());
    }
}
