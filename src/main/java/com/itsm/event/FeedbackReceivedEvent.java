package com.itsm.event;

import com.itsm.enums.AuditAction;
import com.itsm.model.Incident;

/** The reporter rated the resolution. */
public final class FeedbackReceivedEvent extends IncidentEvent {

    public FeedbackReceivedEvent(Incident incident, String actor) {
        super(incident, actor);
    }

    @Override
    public AuditAction getAuditAction() {
        return AuditAction.FEEDBACK_RECEIVED;
    }

    @Override
    public String describe() {
        Incident incident = getIncident();
        return "Reporter rated the resolution " + incident.getFeedbackRating() + "/5"
                + (incident.getFeedbackComment() == null ? "" : ": " + incident.getFeedbackComment());
    }
}
