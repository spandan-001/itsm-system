package com.itsm.service.sla;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itsm.enums.IncidentStatus;
import com.itsm.enums.Priority;
import com.itsm.enums.SlaState;
import com.itsm.enums.SlaType;
import com.itsm.event.SlaBreachedEvent;
import com.itsm.model.Incident;
import com.itsm.model.SlaPolicy;
import com.itsm.repository.IncidentRepository;
import com.itsm.repository.SlaPolicyRepository;

/**
 * Calculates SLA deadlines, stops the clocks on lifecycle transitions and
 * detects breaches. Response is measured to ACKNOWLEDGED; resolution to RESOLVED.
 * Duplicates have no deadlines of their own, so they are never flagged.
 */
@Service
public class SlaService {

    /** Share of the SLA window after which an open clock counts as at risk. */
    static final double AT_RISK_THRESHOLD = 0.75;

    private final SlaPolicyRepository slaPolicyRepository;
    private final IncidentRepository incidentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public SlaService(SlaPolicyRepository slaPolicyRepository,
                      IncidentRepository incidentRepository,
                      ApplicationEventPublisher eventPublisher,
                      Clock clock) {
        this.slaPolicyRepository = slaPolicyRepository;
        this.incidentRepository = incidentRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    // ---- Policies ----

    /** The stored policy for a priority, or the built-in default. */
    @Transactional(readOnly = true)
    public SlaPolicy policyFor(Priority priority) {
        return slaPolicyRepository.findByPriority(priority)
                .orElseGet(() -> SlaPolicy.defaultFor(priority));
    }

    @Transactional(readOnly = true)
    public List<SlaPolicy> allPolicies() {
        return Arrays.stream(Priority.values()).map(this::policyFor).toList();
    }

    /** Changes targets for future incidents. Existing deadlines are not recalculated. */
    @Transactional
    public SlaPolicy updatePolicy(Priority priority, long responseMinutes, long resolutionMinutes) {
        SlaPolicy policy = slaPolicyRepository.findByPriority(priority)
                .orElseGet(() -> new SlaPolicy(priority, responseMinutes, resolutionMinutes));
        policy.updateTargets(responseMinutes, resolutionMinutes);
        return slaPolicyRepository.save(policy);
    }

    // ---- Deadlines ----

    /** Sets both deadlines from the incident's creation time. Call after priority is set. */
    public void applyDeadlines(Incident incident) {
        if (incident.getPriority() == null) {
            throw new IllegalStateException("Priority must be calculated before SLA deadlines");
        }
        SlaPolicy policy = policyFor(incident.getPriority());
        LocalDateTime start = incident.getCreatedAt();
        incident.applySlaDeadlines(
                start.plus(policy.responseTarget()),
                start.plus(policy.resolutionTarget()));
    }

    /** Stops the matching SLA clock when the incident reaches a milestone status. */
    @Transactional
    public void onStatusChange(Incident incident, IncidentStatus newStatus) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (newStatus == IncidentStatus.ACKNOWLEDGED
                && incident.recordResponse(now) && !incident.isResponseBreached()) {
            incident.markResponseBreached();
            eventPublisher.publishEvent(new SlaBreachedEvent(incident, SlaType.RESPONSE, incident.getResponseDueAt()));
        }
        if (newStatus == IncidentStatus.IN_PROGRESS && incident.getResolvedAt() != null) {
            incident.clearResolution(); // reopened: resolution clock runs again
        }
        if (newStatus == IncidentStatus.RESOLVED
                && incident.recordResolution(now) && !incident.isResolutionBreached()) {
            incident.markResolutionBreached();
            eventPublisher.publishEvent(new SlaBreachedEvent(incident, SlaType.RESOLUTION, incident.getResolutionDueAt()));
        }
    }

    // ---- Breach detection ----

    @Transactional
    public int detectBreaches() {
        return detectBreaches(LocalDateTime.now(clock));
    }

    /**
     * Flags every open clock whose deadline has passed and publishes one
     * {@link SlaBreachedEvent} per breach. Each breach is reported once.
     */
    @Transactional
    public int detectBreaches(LocalDateTime now) {
        int count = 0;
        for (Incident incident : incidentRepository
                .findByRespondedAtIsNullAndResponseBreachedFalseAndResponseDueAtBefore(now)) {
            incident.markResponseBreached();
            eventPublisher.publishEvent(new SlaBreachedEvent(incident, SlaType.RESPONSE, incident.getResponseDueAt()));
            count++;
        }
        for (Incident incident : incidentRepository
                .findByResolvedAtIsNullAndResolutionBreachedFalseAndResolutionDueAtBefore(now)) {
            incident.markResolutionBreached();
            eventPublisher.publishEvent(new SlaBreachedEvent(incident, SlaType.RESOLUTION, incident.getResolutionDueAt()));
            count++;
        }
        return count;
    }

    @Transactional(readOnly = true)
    public List<SlaSnapshot> breachedIncidents() {
        LocalDateTime now = LocalDateTime.now(clock);
        return incidentRepository.findByResponseBreachedTrueOrResolutionBreachedTrue().stream()
                .map(incident -> evaluate(incident, now))
                .toList();
    }

    // ---- Evaluation ----

    public SlaSnapshot evaluate(Incident incident) {
        return evaluate(incident, LocalDateTime.now(clock));
    }

    public SlaSnapshot evaluate(Incident incident, LocalDateTime now) {
        LocalDateTime start = incident.getCreatedAt();
        SlaState response = state(start, incident.getResponseDueAt(), incident.getRespondedAt(),
                incident.isResponseBreached(), now);
        SlaState resolution = state(start, incident.getResolutionDueAt(), incident.getResolvedAt(),
                incident.isResolutionBreached(), now);
        Long minutesLeft = (incident.getResolutionDueAt() == null || incident.getResolvedAt() != null)
                ? null
                : Duration.between(now, incident.getResolutionDueAt()).toMinutes();
        return new SlaSnapshot(incident.getId(), incident.getTitle(), incident.getPriority(),
                incident.getStatus(), incident.getResponseDueAt(), incident.getResolutionDueAt(),
                incident.getRespondedAt(), incident.getResolvedAt(), response, resolution, minutesLeft);
    }

    static SlaState state(LocalDateTime start, LocalDateTime due, LocalDateTime completedAt,
                          boolean flaggedBreached, LocalDateTime now) {
        if (due == null) {
            return SlaState.NOT_APPLICABLE;
        }
        if (completedAt != null) {
            return (flaggedBreached || completedAt.isAfter(due)) ? SlaState.BREACHED : SlaState.MET;
        }
        if (flaggedBreached || now.isAfter(due)) {
            return SlaState.BREACHED;
        }
        long window = Duration.between(start, due).toSeconds();
        long elapsed = Duration.between(start, now).toSeconds();
        return (window > 0 && elapsed >= window * AT_RISK_THRESHOLD) ? SlaState.AT_RISK : SlaState.ON_TRACK;
    }
}
