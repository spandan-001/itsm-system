package com.itsm.model;

import java.time.Duration;

import jakarta.persistence.*;

import com.itsm.enums.Priority;

/** Response and resolution targets for one priority level. */
@Entity
@Table(name = "sla_policies")
public class SlaPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private Priority priority;

    @Column(nullable = false)
    private long responseMinutes;

    @Column(nullable = false)
    private long resolutionMinutes;

    protected SlaPolicy() {
        // Required by JPA
    }

    public SlaPolicy(Priority priority, long responseMinutes, long resolutionMinutes) {
        this.priority = priority;
        updateTargets(responseMinutes, resolutionMinutes);
    }

    /** The built-in targets used when no policy row exists for a priority. */
    public static SlaPolicy defaultFor(Priority priority) {
        return switch (priority) {
            case P1 -> new SlaPolicy(priority, 15, 4 * 60);
            case P2 -> new SlaPolicy(priority, 60, 8 * 60);
            case P3 -> new SlaPolicy(priority, 4 * 60, 24 * 60);
            case P4 -> new SlaPolicy(priority, 8 * 60, 72 * 60);
        };
    }

    public final void updateTargets(long responseMinutes, long resolutionMinutes) {
        if (responseMinutes <= 0 || resolutionMinutes <= 0) {
            throw new IllegalArgumentException("SLA targets must be positive");
        }
        if (resolutionMinutes < responseMinutes) {
            throw new IllegalArgumentException("Resolution target cannot be shorter than response target");
        }
        this.responseMinutes = responseMinutes;
        this.resolutionMinutes = resolutionMinutes;
    }

    public Long getId() {
        return id;
    }

    public Priority getPriority() {
        return priority;
    }

    public long getResponseMinutes() {
        return responseMinutes;
    }

    public long getResolutionMinutes() {
        return resolutionMinutes;
    }

    public Duration responseTarget() {
        return Duration.ofMinutes(responseMinutes);
    }

    public Duration resolutionTarget() {
        return Duration.ofMinutes(resolutionMinutes);
    }
}
