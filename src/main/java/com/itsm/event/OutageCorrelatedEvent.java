package com.itsm.event;

import com.itsm.enums.AuditAction;
import com.itsm.model.Incident;
import com.itsm.model.ServiceOutage;

/** An incident was correlated with others on the same service: a new suspected outage, or joining one. */
public final class OutageCorrelatedEvent extends IncidentEvent {

    private final ServiceOutage outage;
    private final boolean newOutage;

    public OutageCorrelatedEvent(Incident incident, ServiceOutage outage, boolean newOutage) {
        super(incident, SYSTEM_ACTOR);
        this.outage = outage;
        this.newOutage = newOutage;
    }

    public ServiceOutage getOutage() {
        return outage;
    }

    public boolean isNewOutage() {
        return newOutage;
    }

    @Override
    public AuditAction getAuditAction() {
        return AuditAction.OUTAGE_CORRELATED;
    }

    @Override
    public String describe() {
        String service = outage.getService().getName();
        return newOutage
                ? "Correlated with " + (outage.getIncidentCount() - 1) + " other recent incident(s) on " + service
                        + ": possible service outage #" + outage.getId()
                : "Part of known outage #" + outage.getId() + " on " + service;
    }
}
