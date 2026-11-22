package com.itsm.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.itsm.model.ServiceOutage;
import com.itsm.repository.IncidentRepository;
import com.itsm.repository.ServiceOutageRepository;
import com.itsm.state.IncidentStates;

/** Service health: suspected outages, active first (administrators). */
@RestController
public class OutageController {

    private final ServiceOutageRepository outages;
    private final IncidentRepository incidents;

    public OutageController(ServiceOutageRepository outages, IncidentRepository incidents) {
        this.outages = outages;
        this.incidents = incidents;
    }

    @GetMapping("/api/outages")
    @Transactional(readOnly = true)
    public List<OutageView> outages() {
        return outages.findTop20ByOrderByStatusAscDetectedAtDesc().stream()
                .map(o -> new OutageView(o.getId(), o.getService().getId(), o.getService().getName(), o.getStatus(),
                        o.getDetectedAt(), o.getResolvedAt(), o.getIncidentCount(),
                        incidents.countOpenInOutage(o.getId(), IncidentStates.closedStatuses())))
                .toList();
    }

    public record OutageView(Long id, Long serviceId, String serviceName, ServiceOutage.Status status,
                             LocalDateTime detectedAt, LocalDateTime resolvedAt, int incidentCount,
                             long openIncidents) {
    }
}
