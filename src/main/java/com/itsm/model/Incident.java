
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
import com.itsm.state.IncidentState;
import com.itsm.state.IncidentStates;
import java.util.Set;

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

    // Duplicate detection: a duplicate points at the original (root) incident
    @ManyToOne
    @JoinColumn(name = "duplicate_of_id")
    private Incident duplicateOf;

    private Double duplicateScore;

    // SLA tracking (set by SlaService, never directly by callers)
    private LocalDateTime responseDueAt;

    private LocalDateTime resolutionDueAt;

    private LocalDateTime respondedAt;

    private LocalDateTime resolvedAt;

    @Column(nullable = false)
    private boolean responseBreached;

    @Column(nullable = false)
    private boolean resolutionBreached;


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

    // Lifecycle (State pattern). Status changes only go through these methods.

    /** The state object for the current status. Not a bean getter, so it is not serialised. */
    public IncidentState currentState() {
        return IncidentStates.of(status);
    }

    /** Statuses this incident may move to next (handy for UI buttons). */
    public Set<IncidentStatus> getAllowedTransitions() {
        return currentState().allowedNext();
    }

    public String getStatusDescription() {
        return currentState().description();
    }

    /**
     * Moves to {@code target} if the current state allows it.
     * @throws com.itsm.exception.InvalidStateTransitionException otherwise
     */
    public void transitionTo(IncidentStatus target) {
        currentState().validateTransition(this, target);
        this.status = target;
    }

    /**
     * Assigns (or, while still ASSIGNED, reassigns) a technician and moves to ASSIGNED.
     * @throws com.itsm.exception.InvalidStateTransitionException if the state forbids it
     */
    public void assignTo(Technician technician) {
        if (technician == null) {
            throw new IllegalArgumentException("Technician is required");
        }
        Technician previous = this.assignedTechnician;
        this.assignedTechnician = technician;
        if (status == IncidentStatus.ASSIGNED) {
            return; // reassignment before acknowledgement
        }
        try {
            transitionTo(IncidentStatus.ASSIGNED);
        } catch (RuntimeException ex) {
            this.assignedTechnician = previous;
            throw ex;
        }
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


    public List<ITService> getAffectedServices() {
        return affectedServices;
    }

    public void setAffectedServices(List<ITService> affectedServices) {
        this.affectedServices = affectedServices == null
                ? new ArrayList<>()
                : new ArrayList<>(affectedServices);
    }

    // SLA behaviour

    public void applySlaDeadlines(LocalDateTime responseDueAt, LocalDateTime resolutionDueAt) {
        this.responseDueAt = responseDueAt;
        this.resolutionDueAt = resolutionDueAt;
    }

    /** Records the first response. Returns true if it came after the deadline. */
    public boolean recordResponse(LocalDateTime at) {
        if (respondedAt != null) {
            return false;
        }
        respondedAt = at;
        return responseDueAt != null && at.isAfter(responseDueAt);
    }

    /** Records the resolution. Returns true if it came after the deadline. */
    public boolean recordResolution(LocalDateTime at) {
        if (resolvedAt != null) {
            return false;
        }
        resolvedAt = at;
        return resolutionDueAt != null && at.isAfter(resolutionDueAt);
    }

    public void markResponseBreached() {
        responseBreached = true;
    }

    public void markResolutionBreached() {
        resolutionBreached = true;
    }

    public LocalDateTime getResponseDueAt() {
        return responseDueAt;
    }

    public LocalDateTime getResolutionDueAt() {
        return resolutionDueAt;
    }

    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public boolean isResponseBreached() {
        return responseBreached;
    }

    public boolean isResolutionBreached() {
        return resolutionBreached;
    }

    /** Reopening a resolved incident restarts the resolution clock. */
    public void clearResolution() {
        resolvedAt = null;
    }

    // Duplicate behaviour

    /** Links this incident to an original. Always links to the root, never to another duplicate. */
    public void markDuplicateOf(Incident original, double score) {
        if (original == null) {
            throw new IllegalArgumentException("Original incident is required");
        }
        Incident root = original.duplicateOf != null ? original.duplicateOf : original;
        if (root == this || (id != null && id.equals(root.getId()))) {
            throw new IllegalArgumentException("An incident cannot duplicate itself");
        }
        this.duplicateOf = root;
        this.duplicateScore = score;
    }

    public void clearDuplicate() {
        this.duplicateOf = null;
        this.duplicateScore = null;
    }

    public boolean isDuplicate() {
        return duplicateOf != null;
    }

    /** The original incident, or null. Not a bean getter, so JSON shows only the id. */
    public Incident duplicateParent() {
        return duplicateOf;
    }

    public Long getDuplicateOfId() {
        return duplicateOf == null ? null : duplicateOf.getId();
    }

    public Double getDuplicateScore() {
        return duplicateScore;
    }
}
