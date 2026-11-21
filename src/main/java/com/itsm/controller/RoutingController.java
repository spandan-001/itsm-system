package com.itsm.controller;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

import com.itsm.dto.StrategyInfo;
import com.itsm.dto.TechnicianUpdateRequests;
import com.itsm.enums.IncidentCategory;
import com.itsm.exception.ResourceNotFoundException;
import com.itsm.model.Technician;
import com.itsm.repository.TechnicianRepository;
import com.itsm.service.IncidentService;
import com.itsm.service.AccountService;
import com.itsm.service.AdminAccess;
import com.itsm.model.AccountLink.Role;
import com.itsm.service.routing.RoutingService;
import com.itsm.strategy.assignment.TechnicianRecommendation;
import com.itsm.strategy.priority.PriorityCalculator;

/** Technician routing, technician skills/availability, and strategy selection. */
@RestController
public class RoutingController {

    private final RoutingService routingService;
    private final PriorityCalculator priorityCalculator;
    private final IncidentService incidentService;
    private final TechnicianRepository technicianRepository;
    private final AccountService accounts;
    private final AdminAccess admins;

    public RoutingController(RoutingService routingService, PriorityCalculator priorityCalculator,
                             IncidentService incidentService, TechnicianRepository technicianRepository,
                             AccountService accounts, AdminAccess admins) {
        this.routingService = routingService;
        this.priorityCalculator = priorityCalculator;
        this.incidentService = incidentService;
        this.technicianRepository = technicianRepository;
        this.accounts = accounts;
        this.admins = admins;
    }

    /** Smart technician recommendation for one incident, best first, with reasons. */
    @GetMapping("/api/incidents/{id}/recommendations")
    public List<TechnicianRecommendation> recommendations(@PathVariable Long id) {
        return routingService.recommend(incidentService.getIncident(id));
    }

    @PostMapping("/api/routing/process-queue")
    public Map<String, Integer> processQueue() {
        return Map.of("assigned", incidentService.routeQueuedIncidents());
    }

    // ---- Technicians ----

    @GetMapping("/api/technicians")
    public List<TechnicianView> technicians() {
        Map<Long, Long> workload = routingService.activeWorkload();
        return technicianRepository.findAll().stream()
                .map(t -> TechnicianView.of(t, workload.getOrDefault(t.getId(), 0L)))
                .toList();
    }

    @GetMapping("/api/technicians/me")
    public TechnicianView currentTechnician() {
        var account = accounts.requireCurrentAccount();
        if (account.role() != Role.TECHNICIAN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return view(technicianRepository.findById(account.id())
                .orElseThrow(() -> new ResourceNotFoundException("Technician", account.id())));
    }

    @PatchMapping("/api/technicians/{id}/availability")
    public TechnicianView setAvailability(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                          @Valid @RequestBody TechnicianUpdateRequests.Availability request) {
        if (!admins.allows(jwt.getSubject())) {
            var account = accounts.requireCurrentAccount();
            if (account.role() != Role.TECHNICIAN || !id.equals(account.id())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
        }
        Technician technician = updateTechnician(id, t -> t.setAvailable(request.available()));
        if (request.available()) {
            incidentService.routeQueuedIncidents(); // a returning technician can pick up queued work
        }
        return view(technician);
    }

    @PutMapping("/api/technicians/{id}/skills")
    public TechnicianView setSkills(@PathVariable Long id,
                                    @Valid @RequestBody TechnicianUpdateRequests.Skills request) {
        return view(updateTechnician(id, t -> {
            for (IncidentCategory category : IncidentCategory.values()) {
                if (request.skills().contains(category)) {
                    t.addSkill(category);
                } else {
                    t.removeSkill(category);
                }
            }
        }));
    }

    // ---- Strategies ----

    @GetMapping("/api/strategies")
    public Map<String, StrategyInfo> strategies() {
        return Map.of(
                "priority", StrategyInfo.of(priorityCalculator.strategies()),
                "assignment", StrategyInfo.of(routingService.strategies()));
    }

    @PutMapping("/api/strategies/priority/{name}")
    public StrategyInfo usePriorityStrategy(@PathVariable String name) {
        priorityCalculator.strategies().use(name);
        return StrategyInfo.of(priorityCalculator.strategies());
    }

    @PutMapping("/api/strategies/assignment/{name}")
    public StrategyInfo useAssignmentStrategy(@PathVariable String name) {
        routingService.strategies().use(name);
        return StrategyInfo.of(routingService.strategies());
    }

    private Technician updateTechnician(Long id, java.util.function.Consumer<Technician> change) {
        Technician technician = technicianRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Technician", id));
        change.accept(technician);
        return technicianRepository.save(technician);
    }

    private TechnicianView view(Technician technician) {
        return TechnicianView.of(technician, routingService.activeWorkload().getOrDefault(technician.getId(), 0L));
    }

    public record TechnicianView(Long id, String name, String email, String domain, boolean available,
                                 java.util.Set<IncidentCategory> skills, long activeIncidents) {
        static TechnicianView of(Technician t, long active) {
            return new TechnicianView(t.getId(), t.getName(), t.getEmail(), t.getDomain(), t.isAvailable(),
                    t.getSkills(), active);
        }
    }
}
