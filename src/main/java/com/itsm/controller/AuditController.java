package com.itsm.controller;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.itsm.service.AccountService;

import com.itsm.dto.AuditLogResponse;
import com.itsm.service.IncidentService;
import com.itsm.service.audit.AuditService;

@RestController
public class AuditController {

    private final AuditService auditService;
    private final IncidentService incidentService;
    private final AccountService accounts;

    public AuditController(AuditService auditService, IncidentService incidentService, AccountService accounts) {
        this.auditService = auditService;
        this.incidentService = incidentService;
        this.accounts = accounts;
    }

    /** Chronological audit trail for one incident. */
    @GetMapping("/api/incidents/{id}/audit")
    public List<AuditLogResponse> incidentTrail(@PathVariable Long id) {
        incidentService.getIncident(id, accounts.requireCurrentAccount()); // 404 if missing
        return auditService.trailFor(id).stream().map(AuditLogResponse::from).toList();
    }

    /** System-wide audit log, newest first. */
    @GetMapping("/api/audit")
    public PagedModel<AuditLogResponse> allEntries(
            @PageableDefault(size = 50, sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return new PagedModel<>(auditService.all(pageable).map(AuditLogResponse::from));
    }
}
