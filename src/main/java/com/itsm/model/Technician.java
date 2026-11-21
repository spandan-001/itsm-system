package com.itsm.model;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.*;

import com.itsm.enums.IncidentCategory;

@Entity
@Table(name = "technicians")
public class Technician extends User {

    @Column(nullable = false)
    private String domain;

    @Column(nullable = false)
    private boolean available;

    /** Incident categories this technician can handle; used by skill-based routing. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "technician_skills", joinColumns = @JoinColumn(name = "technician_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "skill", nullable = false)
    private Set<IncidentCategory> skills = new HashSet<>();

    protected Technician() {
        // Required by JPA
    }

    /** Skills default to the domain when it names a category, e.g. "NETWORK". */
    public Technician(String name, String email,
                      String domain, boolean available) {
        this(name, email, domain, available, categoryFromDomain(domain));
    }

    public Technician(String name, String email, String domain, boolean available,
                      Collection<IncidentCategory> skills) {
        super(name, email);
        this.domain = domain;
        this.available = available;
        this.skills = skills == null ? new HashSet<>() : new HashSet<>(skills);
    }

    private static Set<IncidentCategory> categoryFromDomain(String domain) {
        if (domain == null) {
            return EnumSet.noneOf(IncidentCategory.class);
        }
        try {
            return EnumSet.of(IncidentCategory.valueOf(domain.trim().toUpperCase()));
        } catch (IllegalArgumentException notACategory) {
            return EnumSet.noneOf(IncidentCategory.class);
        }
    }

    public String getDomain() {
        return domain;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public Set<IncidentCategory> getSkills() {
        return Set.copyOf(skills);
    }

    public boolean hasSkill(IncidentCategory category) {
        return skills.contains(category);
    }

    public void addSkill(IncidentCategory category) {
        skills.add(category);
    }

    public void removeSkill(IncidentCategory category) {
        skills.remove(category);
    }
}
