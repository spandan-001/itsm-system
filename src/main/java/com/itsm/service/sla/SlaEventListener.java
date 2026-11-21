package com.itsm.service.sla;

import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.itsm.event.IncidentStatusChangedEvent;

/** Observer that stops SLA clocks when an incident reaches ACKNOWLEDGED or RESOLVED. */
@Component
public class SlaEventListener {

    private final SlaService slaService;

    public SlaEventListener(SlaService slaService) {
        this.slaService = slaService;
    }

    @EventListener
    @Order(2)
    public void onStatusChanged(IncidentStatusChangedEvent event) {
        slaService.onStatusChange(event.getIncident(), event.getToStatus());
    }
}
