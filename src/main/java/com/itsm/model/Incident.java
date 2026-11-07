
package com.itsm.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

import com.itsm.enums.Priority;
import com.itsm.enums.IncidentCategory;
import com.itsm.enums.Impact;
import com.itsm.enums.Urgency;
import com.itsm.enums.IncidentStatus;

@Entity
@Table(name = "incidents")
public class Incident {

    // Primary key
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // User-provided information
    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    // Classification
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Impact impact;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Urgency urgency;

    // System-managed information
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Relationships
    @ManyToOne
    @JoinColumn(name = "reporter_id", nullable = false)
    private Employee reporter;

    @ManyToOne
    @JoinColumn(name = "assigned_technician_id")
    private Technician assignedTechnician;

    @ManyToMany
    @JoinTable(
        name = "incident_services",
        joinColumns = @JoinColumn(name = "incident_id"),
        inverseJoinColumns = @JoinColumn(name = "service_id")
    )
    private List<ITService> affectedServices = new ArrayList<>();

    // Required by JPA
    protected Incident() {
    }

    // Incident creation constructor
    public Incident(
            String title,
            String description,
            IncidentCategory category,
            Impact impact,
            Urgency urgency,
            Employee reporter,
            List<ITService> affectedServices) {

        this.title = title;
        this.description = description;
        this.category = category;
        this.impact = impact;
        this.urgency = urgency;
        this.reporter = reporter;

        this.affectedServices = affectedServices == null
                ? new ArrayList<>()
                : new ArrayList<>(affectedServices);

        this.status = IncidentStatus.NEW;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    // Automatically update timestamp before database update
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public IncidentCategory getCategory() {
        return category;
    }

    public void setCategory(IncidentCategory category) {
        this.category = category;
    }

    public Impact getImpact() {
        return impact;
    }

    public void setImpact(Impact impact) {
        this.impact = impact;
    }

    public Urgency getUrgency() {
        return urgency;
    }

    public void setUrgency(Urgency urgency) {
        this.urgency = urgency;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public void setStatus(IncidentStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Employee getReporter() {
        return reporter;
    }

    public Technician getAssignedTechnician() {
        return assignedTechnician;
    }

    public void setAssignedTechnician(Technician assignedTechnician) {
        this.assignedTechnician = assignedTechnician;
    }

    public List<ITService> getAffectedServices() {
        return affectedServices;
    }

    public void setAffectedServices(List<ITService> affectedServices) {
        this.affectedServices = affectedServices == null
                ? new ArrayList<>()
                : new ArrayList<>(affectedServices);
    }
}
