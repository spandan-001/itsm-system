package com.itsm.controller;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import com.itsm.dto.SlaPolicyResponse;
import com.itsm.dto.UpdateSlaPolicyRequest;
import com.itsm.enums.Priority;
import com.itsm.service.IncidentService;
import com.itsm.service.AccountService;
import com.itsm.service.sla.SlaService;
import com.itsm.service.sla.SlaSnapshot;

@RestController
public class SlaController {

    private final SlaService slaService;
    private final com.itsm.service.escalation.EscalationService escalationService;
    private final com.itsm.service.sla.SlaPredictionService predictions;
    private final IncidentService incidentService;
    private final AccountService accounts;

    public SlaController(SlaService slaService, IncidentService incidentService, AccountService accounts,
                         com.itsm.service.escalation.EscalationService escalationService,
                         com.itsm.service.sla.SlaPredictionService predictions) {
        this.slaService = slaService;
        this.escalationService = escalationService;
        this.predictions = predictions;
        this.incidentService = incidentService;
        this.accounts = accounts;
    }

    @GetMapping("/api/sla/policies")
    public List<SlaPolicyResponse> policies() {
        accounts.requireCurrentAccount();
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
        return slaService.evaluate(incidentService.getIncident(id, accounts.requireCurrentAccount()));
    }

    /** When this incident is likely to be resolved, and whether that beats the deadline. */
    @GetMapping("/api/incidents/{id}/sla-prediction")
    public com.itsm.service.sla.SlaPrediction prediction(@PathVariable Long id) {
        return predictions.predict(incidentService.getIncident(id, accounts.requireCurrentAccount()));
    }

    /** Open incidents predicted to miss their deadline (administrators). */
    @GetMapping("/api/sla/at-risk")
    public List<com.itsm.service.sla.SlaPrediction> atRisk() {
        return predictions.atRisk();
    }

    @GetMapping("/api/sla/breaches")
    public List<SlaSnapshot> breaches() {
        return slaService.breachedIncidents();
    }

    /** Runs breach detection now instead of waiting for the scheduler (handy for demos). */
    @PostMapping("/api/sla/check")
    public Map<String, Integer> checkNow() {
        return Map.of("newWarnings", slaService.detectWarnings(), "newBreaches", slaService.detectBreaches(),
                "newEscalations", escalationService.escalate());
    }
}
