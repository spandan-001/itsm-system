package com.itsm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;

import com.itsm.enums.AuditAction;
import com.itsm.enums.BusinessCriticality;
import com.itsm.enums.Impact;
import com.itsm.enums.IncidentCategory;
import com.itsm.enums.IncidentStatus;
import com.itsm.enums.NotificationChannel;
import com.itsm.enums.Priority;
import com.itsm.enums.Urgency;
import com.itsm.model.AuditLog;
import com.itsm.model.Employee;
import com.itsm.model.ITService;
import com.itsm.model.Incident;
import com.itsm.model.Notification;
import com.itsm.model.Technician;
import com.itsm.repository.AuditLogRepository;
import com.itsm.repository.EmployeeRepository;
import com.itsm.repository.ITServiceRepository;
import com.itsm.repository.IncidentRepository;
import com.itsm.repository.NotificationRepository;
import com.itsm.repository.TechnicianRepository;
import com.itsm.service.IncidentService;
import com.itsm.service.audit.AuditService;
import com.itsm.service.notification.NotificationService;
import com.itsm.service.sla.SlaService;

/**
 * Drives real incidents through IncidentService and checks that the
 * observers (audit, notifications, SLA) react. Not @Transactional on purpose:
 * notifications are only sent after a commit.
 */
@SpringBootTest(properties = "itsm.sla.initial-delay-ms=3600000")
class IncidentObserverIntegrationTest {

    @Autowired IncidentService incidentService;
    @Autowired AuditService auditService;
    @Autowired NotificationService notificationService;
    @Autowired SlaService slaService;
    @Autowired IncidentRepository incidentRepository;
    @Autowired AuditLogRepository auditLogRepository;
    @Autowired NotificationRepository notificationRepository;
    @Autowired EmployeeRepository employeeRepository;
    @Autowired TechnicianRepository technicianRepository;
    @Autowired ITServiceRepository itServiceRepository;

    Employee reporter;
    Technician technician;
    ITService service;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        auditLogRepository.deleteAll();
        incidentRepository.deleteAll();
        employeeRepository.deleteAll();
        technicianRepository.deleteAll();
        itServiceRepository.deleteAll();

        reporter = employeeRepository.save(new Employee("Asha Rao", "asha@example.test", "Finance"));
        technician = technicianRepository.save(new Technician("Ravi Kumar", "ravi@example.test", "NETWORK", true));
        service = itServiceRepository.save(new ITService("Campus Wi-Fi", "Wireless", BusinessCriticality.HIGH));
    }

    private Incident createP1() {
        return incidentService.createIncident("Wi-Fi down", "Library has no Wi-Fi", IncidentCategory.NETWORK,
                Impact.HIGH, Urgency.HIGH, reporter.getId(), List.of(service.getId()));
    }

    @Test
    void creatingAnIncidentSetsSlaAuditsAndNotifiesReporter() {
        Incident incident = createP1();

        assertThat(incident.getPriority()).isEqualTo(Priority.P1);
        assertThat(incident.getResponseDueAt()).isEqualTo(incident.getCreatedAt().plusMinutes(15));
        assertThat(incident.getResolutionDueAt()).isEqualTo(incident.getCreatedAt().plusHours(4));

        List<AuditLog> trail = auditService.trailFor(incident.getId());
        assertThat(trail).singleElement().satisfies(log -> {
            assertThat(log.getAction()).isEqualTo(AuditAction.CREATED);
            assertThat(log.getToStatus()).isEqualTo(IncidentStatus.NEW);
        });

        List<Notification> inbox = notificationService.forRecipient("asha@example.test");
        assertThat(inbox).extracting(Notification::getChannel)
                .containsExactlyInAnyOrder(NotificationChannel.IN_APP, NotificationChannel.EMAIL);
        assertThat(notificationService.unreadCount("asha@example.test")).isEqualTo(1);
    }

    @Test
    void fullLifecycleProducesChronologicalAuditTrailAndStopsSlaClocks() {
        Long id = createP1().getId();
        incidentService.updateStatus(id, IncidentStatus.CLASSIFIED);
        incidentService.assignTechnician(id, technician.getId());
        incidentService.updateStatus(id, IncidentStatus.ACKNOWLEDGED);
        incidentService.updateStatus(id, IncidentStatus.IN_PROGRESS);
        incidentService.updateStatus(id, IncidentStatus.RESOLVED);
        incidentService.updateStatus(id, IncidentStatus.VERIFIED);
        incidentService.updateStatus(id, IncidentStatus.CLOSED);

        assertThat(auditService.trailFor(id)).extracting(AuditLog::getToStatus).containsExactly(
                IncidentStatus.NEW, IncidentStatus.CLASSIFIED, IncidentStatus.ASSIGNED,
                IncidentStatus.ACKNOWLEDGED, IncidentStatus.IN_PROGRESS, IncidentStatus.RESOLVED,
                IncidentStatus.VERIFIED, IncidentStatus.CLOSED);

        Incident closed = incidentService.getIncident(id);
        assertThat(closed.getRespondedAt()).isNotNull();
        assertThat(closed.getResolvedAt()).isNotNull();
        assertThat(closed.isResponseBreached()).isFalse();
        assertThat(closed.isResolutionBreached()).isFalse();

        assertThat(notificationService.forRecipient("ravi@example.test"))
                .extracting(Notification::getSubject).allMatch(s -> s.contains("assigned to you"));
    }

    @Test
    void rejectedTransitionWritesNoAuditEntry() {
        Long id = createP1().getId();

        assertThatThrownBy(() -> incidentService.updateStatus(id, IncidentStatus.CLOSED))
                .isInstanceOf(ResponseStatusException.class);

        assertThat(auditService.trailFor(id)).extracting(AuditLog::getAction).containsExactly(AuditAction.CREATED);
    }

    @Test
    void overdueIncidentIsFlaggedAuditedAndNotifiedExactlyOnce() {
        Long id = createP1().getId();
        incidentService.updateStatus(id, IncidentStatus.CLASSIFIED);
        incidentService.assignTechnician(id, technician.getId());
        LocalDateTime fiveHoursLater = LocalDateTime.now().plusHours(5);

        assertThat(slaService.detectBreaches(fiveHoursLater)).isEqualTo(2); // response + resolution
        assertThat(slaService.detectBreaches(fiveHoursLater)).isZero();     // already flagged

        Incident breached = incidentService.getIncident(id);
        assertThat(breached.isResponseBreached()).isTrue();
        assertThat(breached.isResolutionBreached()).isTrue();
        assertThat(slaService.breachedIncidents()).extracting(s -> s.incidentId()).containsExactly(id);
        assertThat(auditService.trailFor(id)).filteredOn(l -> l.getAction() == AuditAction.SLA_BREACHED).hasSize(2);
        assertThat(notificationService.forRecipient("ravi@example.test"))
                .filteredOn(n -> n.getSubject().contains("SLA breached")).hasSize(4); // 2 breaches x 2 channels
    }
}
