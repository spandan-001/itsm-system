package com.itsm.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import com.itsm.enums.NoteType;

/** One immutable entry on an incident's conversation thread. */
@Entity
@Table(name = "incident_notes", indexes = @Index(name = "idx_incident_notes_incident_id", columnList = "incident_id"))
public class IncidentNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NoteType type;

    @Column(nullable = false)
    private String authorName;

    @Column(nullable = false)
    private String authorEmail;

    @Column(nullable = false, length = 20)
    private String authorRole;

    @Column(nullable = false, length = 4000)
    private String body;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected IncidentNote() {
        // Required by JPA
    }

    public IncidentNote(Incident incident, NoteType type, String authorName, String authorEmail,
                        String authorRole, String body) {
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("Note text is required");
        }
        this.incident = incident;
        this.type = type;
        this.authorName = authorName;
        this.authorEmail = authorEmail;
        this.authorRole = authorRole;
        this.body = body.trim();
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Incident getIncident() {
        return incident;
    }

    public NoteType getType() {
        return type;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getAuthorEmail() {
        return authorEmail;
    }

    public String getAuthorRole() {
        return authorRole;
    }

    public String getBody() {
        return body;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
