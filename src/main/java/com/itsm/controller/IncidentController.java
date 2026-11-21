package com.itsm.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.itsm.dto.CreateIncidentRequest;
import com.itsm.enums.IncidentStatus;
import com.itsm.model.AccountLink.Role;
import com.itsm.model.Incident;
import com.itsm.service.AccountService;
import com.itsm.service.IncidentService;
import com.itsm.service.search.IncidentSearchCriteria;
import com.itsm.service.search.IncidentSearchService;


@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentService incidentService;
    private final AccountService accountService;

    private final IncidentSearchService searchService;

    public IncidentController(IncidentService incidentService, AccountService accountService,
                              IncidentSearchService searchService) {
        this.incidentService = incidentService;
        this.accountService = accountService;
        this.searchService = searchService;
    }

    @GetMapping
    public PagedModel<Incident> listIncidents(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return new PagedModel<>(searchService.search(IncidentSearchCriteria.none(), pageable, accountService.requireCurrentAccount()));
    }

    @GetMapping("/{id}")
    public Incident getIncident(@PathVariable Long id) {
        return incidentService.getIncident(id, accountService.requireCurrentAccount());
    }

    @PostMapping
    public Incident createIncident(@Valid @RequestBody CreateIncidentRequest request) {
        var account = accountService.requireCurrentAccount();
        if (account.role() != Role.EMPLOYEE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only employees can report incidents");
        }
        return incidentService.createIncident(
            request.getTitle(),
            request.getDescription(),
            request.getCategory(),
            request.getImpact(),
            request.getUrgency(),
            account.id(),
            request.getServiceIds()
        );
    }

    @PutMapping("/{id}/assignment")
    public Incident assignTechnician(@PathVariable Long id, @Valid @RequestBody AssignTechnicianRequest request) {
        return incidentService.assignTechnician(id, request.technicianId());
    }

    @PatchMapping("/{id}/status")
    public Incident updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest request) {
        return incidentService.updateStatus(id, request.status(), accountService.requireCurrentAccount());
    }

    public record AssignTechnicianRequest(@NotNull @Positive Long technicianId) {}

    public record UpdateStatusRequest(@NotNull IncidentStatus status) {}
}
