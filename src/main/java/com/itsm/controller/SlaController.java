package com.itsm.controller;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import com.itsm.dto.SlaPolicyResponse;
import com.itsm.dto.UpdateSlaPolicyRequest;
import com.itsm.enums.Priority;
import com.itsm.service.IncidentService;
import com.itsm.service.sla.SlaService;
import com.itsm.service.sla.SlaSnapshot;

@RestController
public class SlaController {

    private final SlaService slaService;
    private final IncidentService incidentService;

    public SlaController(SlaService slaService, IncidentService incidentService) {
        this.slaService = slaService;
        this.incidentService = incidentService;
    }

    @GetMapping("/api/sla/policies")
    public List<SlaPolicyResponse> policies() {
        return slaService.allPolicies().stream().map(SlaPolicyResponse::from).toList();
    }

    @PutMapping("/api/sla/policies/{priority}")
    public SlaPolicyResponse updatePolicy(@PathVariable Priority priority,
                                          @Valid @RequestBody UpdateSlaPolicyRequest request) {
        // An invalid combination throws IllegalArgumentException, mapped to 400.
        return SlaPolicyResponse.from(slaService.updatePolicy(
                priority, request.responseMinutes(), request.resolutionMinutes()));
    }

    @GetMapping("/api/incidents/{id}/sla")
    public SlaSnapshot incidentSla(@PathVariable Long id) {
        return slaService.evaluate(incidentService.getIncident(id));
    }

    @GetMapping("/api/sla/breaches")
    public List<SlaSnapshot> breaches() {
        return slaService.breachedIncidents();
    }

    /** Runs breach detection now instead of waiting for the scheduler (handy for demos). */
    @PostMapping("/api/sla/check")
    public Map<String, Integer> checkNow() {
        return Map.of("newBreaches", slaService.detectBreaches());
    }
}
