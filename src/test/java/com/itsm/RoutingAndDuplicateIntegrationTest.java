package com.itsm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import com.itsm.enums.AuditAction;
import com.itsm.enums.Impact;
import com.itsm.enums.IncidentCategory;
import com.itsm.enums.IncidentStatus;
import com.itsm.enums.Priority;
import com.itsm.enums.Urgency;
import com.itsm.exception.ItsmException;
import com.itsm.model.AuditLog;
import com.itsm.model.Incident;
import com.itsm.model.Technician;
import com.itsm.service.analytics.AnalyticsService;
import com.itsm.service.analytics.AnalyticsSummary;
import com.itsm.service.audit.AuditService;
import com.itsm.service.search.IncidentSearchCriteria;
import com.itsm.service.search.IncidentSearchService;

class RoutingAndDuplicateIntegrationTest extends IntegrationTestSupport {

    @Autowired AuditService auditService;
    @Autowired IncidentSearchService searchService;
    @Autowired AnalyticsService analyticsService;

    @Test
    void incidentIsQueuedWhenNobodyFitsAndRoutedWhenCapacityAppears() {
        networkTech.setAvailable(false);
        technicianRepository.save(networkTech);

        Incident incident = reportWifiOutage();
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.QUEUED);
        assertThat(incident.getAssignedTechnician()).isNull();

        networkTech.setAvailable(true);
        technicianRepository.save(networkTech);
        assertThat(incidentService.routeQueuedIncidents()).isEqualTo(1);

        Incident routed = incidentService.getIncident(incident.getId());
        assertThat(routed.getStatus()).isEqualTo(IncidentStatus.ASSIGNED);
        assertThat(routed.getAssignedTechnician().getId()).isEqualTo(networkTech.getId());
    }

    @Test
    void smartRoutingPrefersTheSkilledTechnician() {
        technicianRepository.save(new Technician("Meera Nair", "meera@example.test", "HARDWARE", true,
                EnumSet.of(IncidentCategory.HARDWARE)));

        Incident network = reportWifiOutage();
        Incident hardware = report("Laptop screen flickers", "Display flickers constantly",
                IncidentCategory.HARDWARE, Impact.LOW, Urgency.MEDIUM);

        assertThat(network.getAssignedTechnician().getName()).isEqualTo("Ravi Kumar");
        assertThat(hardware.getAssignedTechnician().getName()).isEqualTo("Meera Nair");
    }

    @Test
    void similarReportIsLinkedAsDuplicateAndSkipsSlaAndRouting() {
        Incident original = reportWifiOutage();
        Incident duplicate = report("Library wifi not working", "Wi-Fi in the library is down, cannot connect",
                IncidentCategory.NETWORK, Impact.MEDIUM, Urgency.HIGH);

        assertThat(duplicate.isDuplicate()).isTrue();
        assertThat(duplicate.getDuplicateOfId()).isEqualTo(original.getId());
        assertThat(duplicate.getStatus()).isEqualTo(IncidentStatus.NEW);
        assertThat(duplicate.getResolutionDueAt()).isNull();
        assertThat(duplicate.getAssignedTechnician()).isNull();
        assertThat(auditService.trailFor(duplicate.getId())).extracting(AuditLog::getAction)
                .containsExactly(AuditAction.CREATED, AuditAction.DUPLICATE_LINKED);
        assertThatThrownBy(() -> incidentService.updateStatus(duplicate.getId(), IncidentStatus.CLASSIFIED))
                .isInstanceOf(ItsmException.class);
    }

    @Test
    void unrelatedReportIsNotADuplicate() {
        reportWifiOutage();
        Incident other = report("VPN certificate expired", "Remote VPN rejects the certificate",
                IncidentCategory.NETWORK, Impact.MEDIUM, Urgency.MEDIUM);

        assertThat(other.isDuplicate()).isFalse();
        assertThat(other.getStatus()).isEqualTo(IncidentStatus.ASSIGNED);
    }

    @Test
    void markingNotDuplicateGivesItsOwnSlaAndRoutesIt() {
        reportWifiOutage();
        Long id = report("Library wifi not working", "Wi-Fi in the library is down, cannot connect",
                IncidentCategory.NETWORK, Impact.MEDIUM, Urgency.HIGH).getId();

        Incident separate = incidentService.markNotDuplicate(id);

        assertThat(separate.isDuplicate()).isFalse();
        assertThat(separate.getResolutionDueAt()).isNotNull();
        assertThat(separate.getStatus()).isEqualTo(IncidentStatus.ASSIGNED);
    }

    @Test
    void affectedUsersFeedIntoThePriorityOfARealIncident() {
        var portal = itServiceRepository.save(new com.itsm.model.ITService("Student Portal", "Web",
                com.itsm.enums.BusinessCriticality.MEDIUM));
        Incident one = incidentService.createIncident("Portal slow for me", "Pages load slowly",
                IncidentCategory.APPLICATION, Impact.MEDIUM, Urgency.MEDIUM, reporter.getId(),
                java.util.List.of(portal.getId()), 1);
        Incident many = incidentService.createIncident("Registration portal slow for the whole batch",
                "Every student registering today sees timeouts", IncidentCategory.APPLICATION, Impact.MEDIUM,
                Urgency.MEDIUM, reporter.getId(), java.util.List.of(portal.getId()), 400);

        assertThat(one.getPriority()).isEqualTo(Priority.P3);
        assertThat(many.getPriority()).isEqualTo(Priority.P2);
        assertThat(many.getAffectedUsers()).isEqualTo(400);
    }

    @Test
    void searchCombinesFilters() {
        reportWifiOutage();
        report("Printer offline", "Second floor printer is offline", IncidentCategory.HARDWARE,
                Impact.LOW, Urgency.LOW);

        var p1Network = new IncidentSearchCriteria(null, java.util.List.of(Priority.P1), IncidentCategory.NETWORK,
                null, reporter.getId(), wifi.getId(), null, null, "LIBRARY", false, false, null, null, null);
        assertThat(searchService.search(p1Network, PageRequest.of(0, 10)).getContent())
                .extracting(Incident::getTitle).containsExactly("Wi-Fi down in library");

        // No hardware technician exists, so smart routing falls back to Ravi for this P4.
        var ravisHardware = new IncidentSearchCriteria(java.util.List.of(IncidentStatus.ASSIGNED), null,
                IncidentCategory.HARDWARE, networkTech.getId(), null, null, null, null, null, null, null, null, null, null);
        assertThat(searchService.search(ravisHardware, PageRequest.of(0, 10)).getContent())
                .extracting(Incident::getTitle).containsExactly("Printer offline");
    }

    @Test
    void analyticsSummarisesTheWorkload() {
        Long id = reportWifiOutage().getId();
        report("Printer offline", "Second floor printer is offline", IncidentCategory.HARDWARE, Impact.LOW, Urgency.LOW);
        incidentService.updateStatus(id, IncidentStatus.ACKNOWLEDGED);
        incidentService.updateStatus(id, IncidentStatus.IN_PROGRESS);
        incidentService.updateStatus(id, IncidentStatus.RESOLVED, "Restarted the access point");

        AnalyticsSummary summary = analyticsService.summary();

        assertThat(summary.totalIncidents()).isEqualTo(2);
        assertThat(summary.openIncidents()).isEqualTo(1);
        assertThat(summary.byStatus().get(IncidentStatus.RESOLVED)).isEqualTo(1);
        assertThat(summary.byPriority().get(Priority.P1)).isEqualTo(1);
        assertThat(summary.slaCompliancePercent()).isEqualTo(100.0);
        assertThat(summary.technicians()).singleElement().satisfies(t -> {
            assertThat(t.resolvedIncidents()).isEqualTo(1);
            assertThat(t.activeIncidents()).isEqualTo(1);
        });
    }
}
