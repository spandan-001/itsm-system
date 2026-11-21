package com.itsm.service.audit;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itsm.event.IncidentEvent;
import com.itsm.model.AuditLog;
import com.itsm.repository.AuditLogRepository;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /** Writes one audit entry. Works for every event type through polymorphism. */
    @Transactional
    public AuditLog record(IncidentEvent event) {
        AuditLog entry = new AuditLog(
                event.getIncident(),
                event.getAuditAction(),
                event.getFromStatus(),
                event.getToStatus(),
                event.describe(),
                event.getActor(),
                event.getOccurredAt());
        return auditLogRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> trailFor(Long incidentId) {
        return auditLogRepository.findByIncident_IdOrderByOccurredAtAscIdAsc(incidentId);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> all(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }
}
