package com.itsm.service.audit;

import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.itsm.event.IncidentEvent;

/**
 * Observer that writes every incident event to the audit trail. It runs
 * synchronously inside the publisher's transaction, so the audit entry and the
 * incident change are committed or rolled back together.
 */
@Component
public class AuditEventListener {

    private final AuditService auditService;

    public AuditEventListener(AuditService auditService) {
        this.auditService = auditService;
    }

    @EventListener
    @Order(1)
    public void onIncidentEvent(IncidentEvent event) {
        auditService.record(event);
    }
}
