package com.itsm.service;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashSet;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.itsm.enums.*;
import com.itsm.model.ITService;
import com.itsm.repository.EmployeeRepository;
import com.itsm.repository.ITServiceRepository;
import com.itsm.repository.IncidentRepository;
import com.itsm.repository.TechnicianRepository;


import com.itsm.model.Incident;
import com.itsm.model.Employee;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final EmployeeRepository employeeRepository;
    private final ITServiceRepository itServiceRepository;
    private final TechnicianRepository technicianRepository;
   

    public IncidentService(
        IncidentRepository incidentRepository,
        EmployeeRepository employeeRepository,
        ITServiceRepository itServiceRepository,
        TechnicianRepository technicianRepository) {

        this.incidentRepository = incidentRepository;
        this.employeeRepository = employeeRepository;
        this.itServiceRepository = itServiceRepository;
        this.technicianRepository = technicianRepository;
    }

    public Page<Incident> listIncidents(Pageable pageable) {
        return incidentRepository.findAll(pageable);
    }

    public Incident getIncident(Long id) {
        return incidentRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found"));
    }

    @Transactional
    public Incident assignTechnician(Long id, Long technicianId) {
        Incident incident = getIncidentForUpdate(id);
        if (incident.getStatus() == IncidentStatus.ASSIGNED
                && incident.getAssignedTechnician() != null
                && technicianId.equals(incident.getAssignedTechnician().getId())) {
            return incident;
        }
        if (incident.getStatus() != IncidentStatus.CLASSIFIED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Incident must be classified before assignment");
        }

        var technician = technicianRepository.findById(technicianId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Technician not found"));
        if (!technician.isAvailable()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Technician is unavailable");
        }

        incident.setAssignedTechnician(technician);
        incident.setStatus(IncidentStatus.ASSIGNED);
        return incident;
    }

    @Transactional
    public Incident updateStatus(Long id, IncidentStatus status) {
        Incident incident = getIncidentForUpdate(id);
        IncidentStatus current = incident.getStatus();
        if (current == status) {
            return incident;
        }

        IncidentStatus next = switch (current) {
            case NEW -> IncidentStatus.CLASSIFIED;
            case ASSIGNED -> IncidentStatus.ACKNOWLEDGED;
            case ACKNOWLEDGED -> IncidentStatus.IN_PROGRESS;
            case IN_PROGRESS -> IncidentStatus.RESOLVED;
            case RESOLVED -> IncidentStatus.VERIFIED;
            case VERIFIED -> IncidentStatus.CLOSED;
            case CLASSIFIED, CLOSED -> null;
        };
        if (next != status || next == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Cannot change incident status from " + current + " to " + status);
        }
        incident.setStatus(status);
        return incident;
    }

    private Incident getIncidentForUpdate(Long id) {
        return incidentRepository.findLockedById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found"));
    }

    // Convert Impact into a numerical score
    private int getImpactScore(Impact impact) {
        return switch (impact) {
            case LOW -> 1;
            case MEDIUM -> 2;
            case HIGH -> 3;
        };
    }

    // Convert Urgency into a numerical score
    private int getUrgencyScore(Urgency urgency) {
        return switch (urgency) {
            case LOW -> 1;
            case MEDIUM -> 2;
            case HIGH -> 3;
        };
    }

    // Convert BusinessCriticality into a numerical score
    private int getCriticalityScore(BusinessCriticality criticality) {
        return switch (criticality) {
            case LOW -> 1;
            case MEDIUM -> 2;
            case HIGH -> 3;
        };
    }

    // Find the highest criticality among affected services
    private int getHighestCriticality(List<ITService> services) {

        if (services == null || services.isEmpty()) {
            throw new IllegalArgumentException(
                "At least one affected service is required."
            );
        }

        int highest = 1;

        for (ITService service : services) {
            int score = getCriticalityScore(service.getCriticality());
            highest = Math.max(highest, score);
        }

        return highest;
    }

    // Calculate incident priority
    private Priority calculatePriority(
            Impact impact,
            Urgency urgency,
            List<ITService> services) {

        int score = getImpactScore(impact)
                  * getUrgencyScore(urgency)
                  * getHighestCriticality(services);

        if (score >= 18) return Priority.P1;
        if (score >= 9) return Priority.P2;
        if (score >= 4) return Priority.P3;

        return Priority.P4;
    }


    public Incident createIncident(
        String title,
        String description,
        IncidentCategory category,
        Impact impact,
        Urgency urgency,
        Long reporterId,
        List<Long> serviceIds) {

        // 1. Retrieve the existing employee
        Employee reporter = employeeRepository.findById(reporterId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

        // 2. Retrieve the affected IT services
        var uniqueServiceIds = new ArrayList<>(new LinkedHashSet<>(serviceIds));
        List<ITService> affectedServices = itServiceRepository.findAllById(uniqueServiceIds);

        // 3. Validate that all services exist
        if (affectedServices.size() != uniqueServiceIds.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "One or more services were not found");
        }

        // 4. Calculate priority
        Priority priority = calculatePriority(
            impact,
            urgency,
            affectedServices
        );

        // 5. Create the incident object
        Incident incident = new Incident(
            title,
            description,
            category,
            impact,
            urgency,
            reporter,
            affectedServices
        );

        // 6. Set calculated priority
        incident.setPriority(priority);

        // 7. Save to database
        return incidentRepository.save(incident);
    }
}

