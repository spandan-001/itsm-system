package com.itsm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.itsm.enums.AuditAction;
import com.itsm.enums.BusinessCriticality;
import com.itsm.enums.Impact;
import com.itsm.enums.IncidentCategory;
import com.itsm.enums.IncidentStatus;
import com.itsm.enums.Urgency;
import com.itsm.model.ITService;
import com.itsm.model.Incident;
import com.itsm.model.ServiceOutage;
import com.itsm.model.Technician;
import com.itsm.service.analytics.AnalyticsService;
import com.itsm.service.audit.AuditService;
import com.itsm.service.notification.NotificationService;

class OutageDetectionIntegrationTest extends IntegrationTestSupport {

    @Autowired AuditService auditService;
    @Autowired NotificationService notificationService;
    @Autowired AnalyticsService analyticsService;

    private Incident wifiReport(String title, String description) {
        return report(title, description, IncidentCategory.NETWORK, Impact.MEDIUM, Urgency.MEDIUM);
    }

    private void threeWifiReports() {
        wifiReport("Wi-Fi down in library", "No devices can connect");
        wifiReport("Hostel network unreachable", "Laptops show no internet in block C");
        wifiReport("Lecture hall cannot get online", "Projector PC lost its connection");
    }

    @Test
    void threeReportsOnOneServiceRaiseAnOutageAndLinkThemAll() {
        wifiReport("Wi-Fi down in library", "No devices can connect");
        wifiReport("Hostel network unreachable", "Laptops show no internet in block C");
        assertThat(outageRepository.count()).isZero(); // two reports are not a pattern yet

        Incident third = wifiReport("Lecture hall cannot get online", "Projector PC lost its connection");

        ServiceOutage outage = outageRepository.findAll().getFirst();
        assertThat(outage.isActive()).isTrue();
        assertThat(outage.getIncidentCount()).isEqualTo(3);
        assertThat(incidentRepository.findAll()).allMatch(i -> outage.getId().equals(i.getOutageId()));
        assertThat(auditService.trailFor(third.getId())).extracting(l -> l.getAction())
                .contains(AuditAction.OUTAGE_CORRELATED);
        assertThat(notificationService.forRecipient("it.manager@example.test")).anyMatch(n -> n.getSubject()
                .startsWith("Possible outage: Campus Wi-Fi"));
        assertThat(analyticsService.summary().activeOutages()).isEqualTo(1);
    }

    @Test
    void laterReportsJoinTheActiveOutage() {
        threeWifiReports();
        Incident fourth = wifiReport("VPN drops on campus", "Connection keeps falling over");

        assertThat(outageRepository.count()).isEqualTo(1);
        assertThat(outageRepository.findAll().getFirst().getIncidentCount()).isEqualTo(4);
        assertThat(fourth.getOutageId()).isNotNull();
        assertThat(notificationService.forRecipient("asha@example.test"))
                .anyMatch(n -> n.getSubject().contains("part of a known outage"));
    }

    @Test
    void reportsOnDifferentServicesAreNotCorrelated() {
        ITService portal = itServiceRepository.save(new ITService("Student Portal", "Web", BusinessCriticality.MEDIUM));
        ITService email = itServiceRepository.save(new ITService("Email", "Mail", BusinessCriticality.HIGH));
        wifiReport("Wi-Fi down in library", "No devices can connect");
        incidentService.createIncident("Portal slow", "Registration pages time out", IncidentCategory.APPLICATION,
                Impact.MEDIUM, Urgency.MEDIUM, reporter.getId(), java.util.List.of(portal.getId()));
        incidentService.createIncident("Mail bouncing", "Messages to staff bounce", IncidentCategory.APPLICATION,
                Impact.MEDIUM, Urgency.MEDIUM, reporter.getId(), java.util.List.of(email.getId()));

        assertThat(outageRepository.count()).isZero();
    }

    @Test
    void outageResolvesWhenItsLastIncidentIsResolved() {
        technicianRepository.save(new Technician("Second Network Tech", "net2@example.test", "NETWORK", true,
                EnumSet.of(IncidentCategory.NETWORK)));
        threeWifiReports();
        var open = incidentRepository.findAll();

        for (int i = 0; i < open.size(); i++) {
            Long id = open.get(i).getId();
            incidentService.updateStatus(id, IncidentStatus.ACKNOWLEDGED);
            incidentService.updateStatus(id, IncidentStatus.IN_PROGRESS);
            incidentService.updateStatus(id, IncidentStatus.RESOLVED, "Restarted the wireless controller");
            boolean last = i == open.size() - 1;
            assertThat(outageRepository.findAll().getFirst().isActive()).isEqualTo(!last);
        }
        assertThat(outageRepository.findAll().getFirst().getResolvedAt()).isNotNull();
    }
}
