package com.itsm.service.outage;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itsm.event.IncidentCreatedEvent;
import com.itsm.event.IncidentStatusChangedEvent;
import com.itsm.event.OutageCorrelatedEvent;
import com.itsm.model.ITService;
import com.itsm.model.Incident;
import com.itsm.model.ServiceOutage;
import com.itsm.repository.IncidentRepository;
import com.itsm.repository.ServiceOutageRepository;
import com.itsm.state.IncidentStates;

/**
 * Incident correlation (intelligent feature). Several reports against one
 * IT service in a short window usually mean the service itself is down, not
 * several separate problems. When {@code threshold} open incidents (duplicates
 * included) arrive within {@code window}, they are linked to a new
 * {@link ServiceOutage}; later reports on that service join it, and it
 * resolves itself once every linked incident is resolved.
 */
@Service
public class OutageDetector {

    private final IncidentRepository incidents;
    private final ServiceOutageRepository outages;
    private final ApplicationEventPublisher eventPublisher;
    private final int threshold;
    private final Duration window;

    public OutageDetector(IncidentRepository incidents, ServiceOutageRepository outages,
                          ApplicationEventPublisher eventPublisher,
                          @Value("${itsm.outage.threshold:3}") int threshold,
                          @Value("${itsm.outage.window-minutes:30}") long windowMinutes) {
        this.incidents = incidents;
        this.outages = outages;
        this.eventPublisher = eventPublisher;
        this.threshold = threshold;
        this.window = Duration.ofMinutes(windowMinutes);
    }

    /** Runs inside the creating transaction, so the new incident is linked before it commits. */
    @EventListener
    @Order(4)
    @Transactional
    public void onIncidentCreated(IncidentCreatedEvent event) {
        Incident incident = event.getIncident();
        for (ITService service : incident.getAffectedServices()) {
            var active = outages.findFirstByService_IdAndStatus(service.getId(), ServiceOutage.Status.ACTIVE);
            if (active.isPresent()) {
                incident.linkToOutage(active.get());
                active.get().recordAnotherIncident();
                eventPublisher.publishEvent(new OutageCorrelatedEvent(incident, active.get(), false));
                return;
            }
            LocalDateTime since = incident.getCreatedAt().minus(window);
            List<Incident> recent = incidents.findRecentOpenOnService(service.getId(), since,
                    IncidentStates.closedStatuses());
            if (recent.size() >= threshold) {
                ServiceOutage outage = outages.save(new ServiceOutage(service, recent.size()));
                recent.forEach(i -> i.linkToOutage(outage));
                eventPublisher.publishEvent(new OutageCorrelatedEvent(incident, outage, true));
                return;
            }
        }
    }

    /** An outage is over once none of its incidents is still open. */
    @EventListener
    @Transactional
    public void onStatusChanged(IncidentStatusChangedEvent event) {
        ServiceOutage outage = event.getIncident().outage();
        if (outage == null || !outage.isActive() || !IncidentStates.closedStatuses().contains(event.getToStatus())) {
            return;
        }
        if (incidents.countOpenInOutage(outage.getId(), IncidentStates.closedStatuses()) == 0) {
            outage.resolve();
        }
    }

    /** For callers outside the event flow (e.g. tests): whether an active outage exists on a service. */
    @Transactional(readOnly = true)
    public boolean hasActiveOutage(Long serviceId) {
        return outages.findFirstByService_IdAndStatus(serviceId, ServiceOutage.Status.ACTIVE).isPresent();
    }
}
