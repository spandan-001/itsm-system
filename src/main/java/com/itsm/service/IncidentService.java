package com.itsm.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.itsm.enums.*;
import com.itsm.model.ITService;
import com.itsm.repository.EmployeeRepository;
import com.itsm.repository.ITServiceRepository;
import com.itsm.repository.IncidentRepository;


import com.itsm.model.Incident;
import com.itsm.model.Employee;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final EmployeeRepository employeeRepository;
    private final ITServiceRepository itServiceRepository;
   

    public IncidentService(
        IncidentRepository incidentRepository,
        EmployeeRepository employeeRepository,
        ITServiceRepository itServiceRepository) {

        this.incidentRepository = incidentRepository;
        this.employeeRepository = employeeRepository;
        this.itServiceRepository = itServiceRepository;
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
            .orElseThrow(() -> new RuntimeException("Employee not found"));

        // 2. Retrieve the affected IT services
        List<ITService> affectedServices =
            itServiceRepository.findAllById(serviceIds);

        // 3. Validate that all services exist
        if (affectedServices.size() != serviceIds.size()) {
            throw new RuntimeException("One or more services were not found");
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

