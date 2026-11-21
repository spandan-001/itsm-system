package com.itsm.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.*;

import com.itsm.dto.CreateIncidentRequest;
import com.itsm.enums.IncidentStatus;
import com.itsm.model.Incident;
import com.itsm.service.IncidentService;


@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @GetMapping
    public PagedModel<Incident> listIncidents(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return new PagedModel<>(incidentService.listIncidents(pageable));
    }

    @GetMapping("/{id}")
    public Incident getIncident(@PathVariable Long id) {
        return incidentService.getIncident(id);
    }

    @PostMapping
    public Incident createIncident(@Valid @RequestBody CreateIncidentRequest request) {
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

    @PutMapping("/{id}/assignment")
    public Incident assignTechnician(@PathVariable Long id, @Valid @RequestBody AssignTechnicianRequest request) {
        return incidentService.assignTechnician(id, request.technicianId());
    }

    @PatchMapping("/{id}/status")
    public Incident updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest request) {
        return incidentService.updateStatus(id, request.status());
    }

    public record AssignTechnicianRequest(@NotNull @Positive Long technicianId) {}

    public record UpdateStatusRequest(@NotNull IncidentStatus status) {}
}

