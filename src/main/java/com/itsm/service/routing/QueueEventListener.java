package com.itsm.service.routing;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.itsm.enums.IncidentStatus;
import com.itsm.event.IncidentStatusChangedEvent;
import com.itsm.service.IncidentService;

/**
 * Retries queued incidents when capacity frees up (an incident is resolved),
 * and periodically as a safety net. No synchronous retry loops.
 */
@Component
public class QueueEventListener {

    private final IncidentService incidentService;

    public QueueEventListener(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onStatusChanged(IncidentStatusChangedEvent event) {
        if (event.getToStatus() == IncidentStatus.RESOLVED) {
            incidentService.routeQueuedIncidents();
        }
    }

    @Scheduled(initialDelayString = "${itsm.routing.initial-delay-ms:30000}",
               fixedDelayString = "${itsm.routing.queue-interval-ms:60000}")
    public void periodicRetry() {
        incidentService.routeQueuedIncidents();
    }
}
