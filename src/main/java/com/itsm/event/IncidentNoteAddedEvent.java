package com.itsm.event;

import com.itsm.enums.AuditAction;
import com.itsm.model.IncidentNote;

/** Someone added to the incident's conversation thread. */
public final class IncidentNoteAddedEvent extends IncidentEvent {

    private static final int AUDIT_PREVIEW = 200;

    private final IncidentNote note;

    public IncidentNoteAddedEvent(IncidentNote note, String actor) {
        super(note.getIncident(), actor);
        this.note = note;
    }

    public IncidentNote getNote() {
        return note;
    }

    @Override
    public AuditAction getAuditAction() {
        return AuditAction.NOTE_ADDED;
    }

    @Override
    public String describe() {
        String who = note.getAuthorName() + " (" + note.getAuthorRole() + ")";
        String kind = switch (note.getType()) {
            case ADDITIONAL_INFO -> "Additional information from ";
            case INFO_REQUEST -> "Information requested by ";
            case INFO_RESPONSE -> "Information provided by ";
            case INVESTIGATION -> "Investigation note by ";
            case RESOLUTION -> "Resolution provided by ";
            case REOPEN_REASON -> "Reopened by ";
            case STATUS_COMMENT -> "Comment by ";
        };
        // Internal notes are not copied into the audit trail, which reporters can read.
        if (note.getType().isInternal()) {
            return kind + who;
        }
        String body = note.getBody();
        return kind + who + ": " + (body.length() > AUDIT_PREVIEW ? body.substring(0, AUDIT_PREVIEW) + "…" : body);
    }
}
