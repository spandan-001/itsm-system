package com.itsm.event;

import com.itsm.enums.AuditAction;
import com.itsm.model.Incident;

/** An incident was linked to (or unlinked from) an original as a duplicate. */
public final class DuplicateLinkEvent extends IncidentEvent {

    private final Incident original;
    private final Double score;
    private final boolean linked;

    private DuplicateLinkEvent(Incident incident, Incident original, Double score, boolean linked, String actor) {
        super(incident, actor);
        this.original = original;
        this.score = score;
        this.linked = linked;
    }

    public static DuplicateLinkEvent linked(Incident incident, Incident original, double score) {
        return new DuplicateLinkEvent(incident, original, score, true, SYSTEM_ACTOR);
    }

    public static DuplicateLinkEvent unlinked(Incident incident, Incident formerOriginal, String actor) {
        return new DuplicateLinkEvent(incident, formerOriginal, null, false, actor);
    }

    public Incident getOriginal() {
        return original;
    }

    public boolean isLinked() {
        return linked;
    }

    @Override
    public AuditAction getAuditAction() {
        return linked ? AuditAction.DUPLICATE_LINKED : AuditAction.DUPLICATE_UNLINKED;
    }

    @Override
    public String describe() {
        return linked
                ? String.format("Detected as duplicate of incident #%d (similarity %.0f%%)", original.getId(), score * 100)
                : "Marked as not a duplicate of incident #" + original.getId();
    }
}
