package com.itsm.controller;

import org.springframework.web.bind.annotation.*;

import com.itsm.dto.CreateIncidentRequest;
import com.itsm.model.Incident;
import com.itsm.service.IncidentService;


@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @PostMapping
    public Incident createIncident(@RequestBody CreateIncidentRequest request) {
        return incidentService.createIncident(
            request.getTitle(),
            request.getDescription(),
            request.getCategory(),
            request.getImpact(),
            request.getUrgency(),
            request.getReporterId(),
            request.getServiceIds()
        );
    }
}

