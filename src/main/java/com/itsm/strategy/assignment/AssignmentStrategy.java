package com.itsm.strategy.assignment;

import java.util.List;

import com.itsm.model.Incident;
import com.itsm.strategy.NamedStrategy;

/** Strategy pattern: one way of choosing a technician for an incident. */
public interface AssignmentStrategy extends NamedStrategy {

    /**
     * Ranks the technicians allowed to take the incident, best first.
     * An empty list means nobody is suitable and the incident should be queued.
     */
    List<TechnicianRecommendation> rank(Incident incident, List<TechnicianCandidate> candidates);
}
