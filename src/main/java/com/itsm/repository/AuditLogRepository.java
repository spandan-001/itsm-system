package com.itsm.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.itsm.model.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByIncident_IdOrderByOccurredAtAscIdAsc(Long incidentId);
}
