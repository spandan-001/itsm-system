package com.itsm.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;

/** A suspected outage of one IT service, raised when several incidents arrive close together. */
@Entity
@Table(name = "service_outages")
public class ServiceOutage {

    public enum Status { ACTIVE, RESOLVED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private ITService service;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(nullable = false)
    private LocalDateTime detectedAt;

    private LocalDateTime resolvedAt;

    @Column(nullable = false)
    private int incidentCount;

    protected ServiceOutage() {
        // Required by JPA
    }

    public ServiceOutage(ITService service, int incidentCount) {
        this.service = service;
        this.status = Status.ACTIVE;
        this.detectedAt = LocalDateTime.now();
        this.incidentCount = incidentCount;
    }

    public void recordAnotherIncident() {
        incidentCount++;
    }

    public void resolve() {
        if (status == Status.ACTIVE) {
            status = Status.RESOLVED;
            resolvedAt = LocalDateTime.now();
        }
    }

    public boolean isActive() {
        return status == Status.ACTIVE;
    }

    public Long getId() {
        return id;
    }

    public ITService getService() {
        return service;
    }

    public Status getStatus() {
        return status;
    }

    public LocalDateTime getDetectedAt() {
        return detectedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public int getIncidentCount() {
        return incidentCount;
    }
}
