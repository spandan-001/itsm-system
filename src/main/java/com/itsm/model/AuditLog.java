package com.itsm.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import com.itsm.enums.AuditAction;
import com.itsm.enums.IncidentStatus;

/**
 * One immutable entry in an incident's audit trail. There are no setters:
 * audit entries are append-only.
 */
@Entity
@Table(name = "audit_logs", indexes = @Index(name = "idx_audit_logs_incident_id", columnList = "incident_id"))
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    private IncidentStatus fromStatus;

    @Enumerated(EnumType.STRING)
    private IncidentStatus toStatus;

    @Column(nullable = false, length = 1000)
    private String details;

    @Column(nullable = false)
    private String actor;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    protected AuditLog() {
        // Required by JPA
    }

    public AuditLog(Incident incident, AuditAction action, IncidentStatus fromStatus,
                    IncidentStatus toStatus, String details, String actor, LocalDateTime occurredAt) {
        this.incident = incident;
        this.action = action;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.details = details;
        this.actor = actor;
        this.occurredAt = occurredAt;
    }

    public Long getId() {
        return id;
    }

    public Incident getIncident() {
        return incident;
    }

    public AuditAction getAction() {
        return action;
    }

    public IncidentStatus getFromStatus() {
        return fromStatus;
    }

    public IncidentStatus getToStatus() {
        return toStatus;
    }

    public String getDetails() {
        return details;
    }

    public String getActor() {
        return actor;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
