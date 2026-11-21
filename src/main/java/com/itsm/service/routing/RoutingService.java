package com.itsm.service.routing;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itsm.model.Incident;
import com.itsm.model.Technician;
import com.itsm.repository.IncidentRepository;
import com.itsm.repository.IncidentRepository.TechnicianCount;
import com.itsm.repository.TechnicianRepository;
import com.itsm.state.IncidentStates;
import com.itsm.strategy.StrategyRegistry;
import com.itsm.strategy.assignment.AssignmentStrategy;
import com.itsm.strategy.assignment.TechnicianCandidate;
import com.itsm.strategy.assignment.TechnicianRecommendation;

/** Strategy context for technician routing: builds live candidates and asks the active strategy. */
@Service
public class RoutingService {

    private final StrategyRegistry<AssignmentStrategy> registry;
    private final TechnicianRepository technicianRepository;
    private final IncidentRepository incidentRepository;

    public RoutingService(List<AssignmentStrategy> strategies,
                          @Value("${itsm.routing.strategy:smart}") String defaultStrategy,
                          TechnicianRepository technicianRepository,
                          IncidentRepository incidentRepository) {
        this.registry = new StrategyRegistry<>("assignment", strategies, defaultStrategy);
        this.technicianRepository = technicianRepository;
        this.incidentRepository = incidentRepository;
    }

    public StrategyRegistry<AssignmentStrategy> strategies() {
        return registry;
    }

    /** Technicians ranked for this incident by the active strategy. */
    @Transactional(readOnly = true)
    public List<TechnicianRecommendation> recommend(Incident incident) {
        return registry.active().rank(incident, candidatesFor(incident));
    }

    /** The best technician, or empty if the incident should be queued. */
    @Transactional(readOnly = true)
    public Optional<Technician> selectTechnician(Incident incident) {
        List<TechnicianCandidate> candidates = candidatesFor(incident);
        return registry.active().rank(incident, candidates).stream()
                .findFirst()
                .flatMap(best -> candidates.stream()
                        .map(TechnicianCandidate::technician)
                        .filter(t -> t.getId().equals(best.technicianId()))
                        .findFirst());
    }

    /** Every technician with their current workload and track record for the incident's category. */
    @Transactional(readOnly = true)
    public List<TechnicianCandidate> candidatesFor(Incident incident) {
        Map<Long, Long> active = toMap(incidentRepository
                .countByTechnicianAndStatusIn(IncidentStates.activeWorkStatuses()));
        Map<Long, Long> experience = toMap(incidentRepository.countResolvedByTechnician(incident.getCategory()));
        return technicianRepository.findAll().stream()
                .map(t -> new TechnicianCandidate(t,
                        active.getOrDefault(t.getId(), 0L).intValue(),
                        experience.getOrDefault(t.getId(), 0L).intValue()))
                .toList();
    }

    /** Current open-incident count for every technician who has any. */
    @Transactional(readOnly = true)
    public Map<Long, Long> activeWorkload() {
        return toMap(incidentRepository.countByTechnicianAndStatusIn(IncidentStates.activeWorkStatuses()));
    }

    private static Map<Long, Long> toMap(List<TechnicianCount> counts) {
        return counts.stream().collect(Collectors.toMap(TechnicianCount::getTechnicianId, TechnicianCount::getTotal));
    }
}
