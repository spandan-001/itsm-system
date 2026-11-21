package com.itsm.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.itsm.enums.Impact;
import com.itsm.enums.IncidentCategory;
import com.itsm.enums.IncidentStatus;
import com.itsm.enums.Urgency;
import com.itsm.event.DuplicateLinkEvent;
import com.itsm.event.IncidentAssignedEvent;
import com.itsm.event.IncidentCreatedEvent;
import com.itsm.event.IncidentStatusChangedEvent;
import com.itsm.exception.ItsmException;
import com.itsm.exception.ResourceNotFoundException;
import com.itsm.exception.TechnicianUnavailableException;
import com.itsm.model.Employee;
import com.itsm.model.ITService;
import com.itsm.model.Incident;
import com.itsm.model.Technician;
import com.itsm.repository.EmployeeRepository;
import com.itsm.repository.ITServiceRepository;
import com.itsm.repository.IncidentRepository;
import com.itsm.repository.TechnicianRepository;
import com.itsm.service.duplicate.DuplicateDetector;
import com.itsm.service.routing.RoutingService;
import com.itsm.service.sla.SlaService;
import com.itsm.strategy.priority.PriorityCalculator;

/**
 * Orchestrates the incident workflow. Business rules live elsewhere:
 * transitions in the State classes, priority and routing in Strategies,
 * and audit/notifications/SLA clocks in Observers reacting to the events
 * published here.
 *
 * <p>Create flow: validate → priority → duplicate check (a duplicate is
 * linked and stops here) → SLA deadlines → save → classify → route to a
 * technician, or QUEUED if none fits.
 */
@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final EmployeeRepository employeeRepository;
    private final ITServiceRepository itServiceRepository;
    private final TechnicianRepository technicianRepository;
    private final SlaService slaService;
    private final PriorityCalculator priorityCalculator;
    private final DuplicateDetector duplicateDetector;
    private final RoutingService routingService;
    private final CurrentActorProvider actorProvider;
    private final ApplicationEventPublisher eventPublisher;
    private final boolean autoRouting;

    public IncidentService(
            IncidentRepository incidentRepository,
            EmployeeRepository employeeRepository,
            ITServiceRepository itServiceRepository,
            TechnicianRepository technicianRepository,
            SlaService slaService,
            PriorityCalculator priorityCalculator,
            DuplicateDetector duplicateDetector,
            RoutingService routingService,
            CurrentActorProvider actorProvider,
            ApplicationEventPublisher eventPublisher,
            @Value("${itsm.routing.auto-assign:true}") boolean autoRouting) {

        this.incidentRepository = incidentRepository;
        this.employeeRepository = employeeRepository;
        this.itServiceRepository = itServiceRepository;
        this.technicianRepository = technicianRepository;
        this.slaService = slaService;
        this.priorityCalculator = priorityCalculator;
        this.duplicateDetector = duplicateDetector;
        this.routingService = routingService;
        this.actorProvider = actorProvider;
        this.eventPublisher = eventPublisher;
        this.autoRouting = autoRouting;
    }

    // ---- Queries ----

    public Incident getIncident(Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", id));
    }

    @Transactional(readOnly = true)
    public Incident getIncident(Long id, AccountService.LinkedAccount account) {
        Incident incident = getIncident(id);
        account.requireAccess(incident);
        return incident;
    }

    /** Incidents linked to this one as duplicates. */
    @Transactional(readOnly = true)
    public List<Incident> duplicatesOf(Long id) {
        getIncident(id);
        return incidentRepository.findByDuplicateOf_Id(id);
    }

    // ---- Creation ----

    @Transactional
    public Incident createIncident(
            String title,
            String description,
            IncidentCategory category,
            Impact impact,
            Urgency urgency,
            Long reporterId,
            List<Long> serviceIds) {

        Employee reporter = employeeRepository.findById(reporterId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", reporterId));

        var uniqueServiceIds = new ArrayList<>(new LinkedHashSet<>(serviceIds));
        List<ITService> affectedServices = itServiceRepository.findAllById(uniqueServiceIds);
        if (affectedServices.size() != uniqueServiceIds.size()) {
            throw new ResourceNotFoundException("One or more services", uniqueServiceIds);
        }

        Incident incident = new Incident(title, description, category, impact, urgency, reporter, affectedServices);
        incident.setPriority(priorityCalculator.calculate(impact, urgency, affectedServices));

        // A duplicate is linked to the original and not given its own SLA or technician.
        Optional<DuplicateDetector.ScoredIncident> duplicate = duplicateDetector.findDuplicate(incident);
        if (duplicate.isPresent()) {
            incident.markDuplicateOf(duplicate.get().incident(), duplicate.get().score());
            Incident saved = incidentRepository.save(incident);
            eventPublisher.publishEvent(new IncidentCreatedEvent(saved, actor()));
            eventPublisher.publishEvent(DuplicateLinkEvent.linked(
                    saved, saved.duplicateParent(), duplicate.get().score()));
            return saved;
        }

        slaService.applyDeadlines(incident);
        Incident saved = incidentRepository.save(incident);
        eventPublisher.publishEvent(new IncidentCreatedEvent(saved, actor()));
        if (autoRouting) {
            classifyAndRoute(saved);
        }
        return saved;
    }

    /** Reporter or admin says "this is not a duplicate": give it its own SLA and route it. */
    @Transactional
    public Incident markNotDuplicate(Long id) {
        Incident incident = getIncidentForUpdate(id);
        if (!incident.isDuplicate()) {
            return incident;
        }
        Incident formerOriginal = incident.duplicateParent();
        incident.clearDuplicate();
        slaService.applyDeadlines(incident);
        eventPublisher.publishEvent(DuplicateLinkEvent.unlinked(incident, formerOriginal, actor()));
        if (autoRouting && incident.getStatus() == IncidentStatus.NEW) {
            classifyAndRoute(incident);
        }
        return incident;
    }

    // ---- Assignment and routing ----

    @Transactional
    public Incident assignTechnician(Long id, Long technicianId) {
        Incident incident = getIncidentForUpdate(id);
        if (incident.getAssignedTechnician() != null
                && technicianId.equals(incident.getAssignedTechnician().getId())
                && incident.getStatus() == IncidentStatus.ASSIGNED) {
            return incident;
        }
        Technician technician = technicianRepository.findById(technicianId)
                .orElseThrow(() -> new ResourceNotFoundException("Technician", technicianId));
        if (!technician.isAvailable()) {
            throw new TechnicianUnavailableException(technician.getName(), "marked unavailable");
        }
        assign(incident, technician);
        return incident;
    }

    /** Tries to route every queued incident, highest priority first. Returns how many were assigned. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int routeQueuedIncidents() {
        List<Incident> queued = new ArrayList<>(incidentRepository.findByStatusOrderByCreatedAtAsc(IncidentStatus.QUEUED));
        queued.sort(Comparator.comparing(Incident::getPriority));
        int assigned = 0;
        for (Incident incident : queued) {
            Optional<Technician> technician = routingService.selectTechnician(incident);
            if (technician.isPresent()) {
                assign(incident, technician.get());
                assigned++;
            }
        }
        return assigned;
    }

    private void classifyAndRoute(Incident incident) {
        if (incident.getStatus() == IncidentStatus.NEW) {
            transition(incident, IncidentStatus.CLASSIFIED);
        }
        routingService.selectTechnician(incident).ifPresentOrElse(
                technician -> assign(incident, technician),
                () -> transition(incident, IncidentStatus.QUEUED));
    }

    private void assign(Incident incident, Technician technician) {
        IncidentStatus previous = incident.getStatus();
        incident.assignTo(technician);
        eventPublisher.publishEvent(new IncidentAssignedEvent(incident, previous, technician, actor()));
    }

    // ---- Lifecycle ----

    /**
     * Moves an incident to the requested status if its current state allows it.
     * @throws com.itsm.exception.InvalidStateTransitionException for an illegal move
     */
    @Transactional
    public Incident updateStatus(Long id, IncidentStatus status) {
        return updateStatus(getIncidentForUpdate(id), status);
    }

    private Incident updateStatus(Incident incident, IncidentStatus status) {
        if (incident.getStatus() == status) {
            return incident;
        }
        if (incident.isDuplicate()) {
            throw new DuplicateLockedException(incident);
        }
        transition(incident, status);
        return incident;
    }

    @Transactional
    public Incident updateStatus(Long id, IncidentStatus status, AccountService.LinkedAccount account) {
        Incident incident = getIncidentForUpdate(id);
        account.requireAccess(incident);
        boolean allowed = switch (account.role()) {
            case EMPLOYEE -> status == IncidentStatus.VERIFIED || status == IncidentStatus.CLOSED;
            case TECHNICIAN -> status == IncidentStatus.ACKNOWLEDGED
                    || status == IncidentStatus.IN_PROGRESS || status == IncidentStatus.RESOLVED;
        };
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This role cannot set the requested status");
        }
        return updateStatus(incident, status);
    }

    private void transition(Incident incident, IncidentStatus target) {
        IncidentStatus from = incident.getStatus();
        incident.transitionTo(target);
        eventPublisher.publishEvent(new IncidentStatusChangedEvent(incident, from, target, actor()));
    }

    private Incident getIncidentForUpdate(Long id) {
        return incidentRepository.findLockedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", id));
    }

    private String actor() {
        return actorProvider.currentActor();
    }

    /** A linked duplicate follows its original and is not worked on separately. */
    static class DuplicateLockedException extends ItsmException {
        DuplicateLockedException(Incident incident) {
            super("Incident #" + incident.getId() + " is a duplicate of #" + incident.getDuplicateOfId()
                    + ". Work on the original, or mark it as not a duplicate first.");
        }
    }
}
