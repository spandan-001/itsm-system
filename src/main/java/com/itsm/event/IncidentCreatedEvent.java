package com.itsm.event;

import com.itsm.enums.AuditAction;
import com.itsm.enums.IncidentStatus;
import com.itsm.model.Incident;

public final class IncidentCreatedEvent extends IncidentEvent {

    public IncidentCreatedEvent(Incident incident, String actor) {
        super(incident, actor);
    }

    @Override
    public AuditAction getAuditAction() {
        return AuditAction.CREATED;
    }

    @Override
    public IncidentStatus getToStatus() {
        return getIncident().getStatus();
    }

    @Override
    public String describe() {
        Incident incident = getIncident();
        String text = "Incident reported by " + incident.getReporter().getName()
                + " with priority " + incident.getPriority()
                + " (impact " + incident.getImpact() + ", urgency " + incident.getUrgency()
                + ", " + incident.getAffectedUsers() + " affected " + (incident.getAffectedUsers() == 1 ? "user" : "users") + ")";
        if (incident.getSuggestedCategory() != null) {
            long percent = Math.round(incident.getClassificationConfidence() * 100);
            text += incident.isAutoReclassified()
                    ? ". Reclassified from " + incident.getReportedCategory() + " to " + incident.getCategory()
                            + " by the classifier (" + percent + "% confident)"
                    : ". Classifier suggested " + incident.getSuggestedCategory() + " (" + percent + "%)";
        }
        return text;
    }
}
