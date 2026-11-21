package com.itsm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.itsm.enums.AuditAction;
import com.itsm.enums.IncidentStatus;
import com.itsm.enums.NotificationChannel;
import com.itsm.enums.Priority;
import com.itsm.exception.InvalidStateTransitionException;
import com.itsm.model.AuditLog;
import com.itsm.model.Incident;
import com.itsm.model.Notification;
import com.itsm.service.audit.AuditService;
import com.itsm.service.notification.NotificationService;
import com.itsm.service.sla.SlaService;

/** Observers (audit, notifications, SLA) reacting to real incidents. */
class IncidentObserverIntegrationTest extends IntegrationTestSupport {

    @Autowired AuditService auditService;
    @Autowired NotificationService notificationService;
    @Autowired SlaService slaService;

    @Test
    void creatingAnIncidentSetsSlaAuditsAndNotifies() {
        Incident incident = reportWifiOutage();

        assertThat(incident.getPriority()).isEqualTo(Priority.P1);
        assertThat(incident.getResponseDueAt()).isEqualTo(incident.getCreatedAt().plusMinutes(15));
        assertThat(incident.getResolutionDueAt()).isEqualTo(incident.getCreatedAt().plusHours(4));

        // Auto-routing: created, classified, then assigned to the skilled technician.
        assertThat(auditService.trailFor(incident.getId())).extracting(AuditLog::getAction)
                .containsExactly(AuditAction.CREATED, AuditAction.STATUS_CHANGED, AuditAction.ASSIGNED);

        List<Notification> inbox = notificationService.forRecipient("asha@example.test");
        assertThat(inbox).extracting(Notification::getChannel)
                .contains(NotificationChannel.IN_APP, NotificationChannel.EMAIL);
        assertThat(notificationService.forRecipient("ravi@example.test"))
                .extracting(Notification::getSubject).allMatch(s -> s.contains("assigned to you"));
    }

    @Test
    void fullLifecycleProducesChronologicalAuditTrailAndStopsSlaClocks() {
        Long id = reportWifiOutage().getId();
        for (IncidentStatus next : List.of(IncidentStatus.ACKNOWLEDGED, IncidentStatus.IN_PROGRESS,
                IncidentStatus.RESOLVED, IncidentStatus.VERIFIED, IncidentStatus.CLOSED)) {
            incidentService.updateStatus(id, next, next == IncidentStatus.RESOLVED ? "Restarted the access point" : null);
        }

        assertThat(auditService.trailFor(id)).filteredOn(l -> l.getToStatus() != null)
                .extracting(AuditLog::getToStatus).containsExactly(
                IncidentStatus.NEW, IncidentStatus.CLASSIFIED, IncidentStatus.ASSIGNED,
                IncidentStatus.ACKNOWLEDGED, IncidentStatus.IN_PROGRESS, IncidentStatus.RESOLVED,
                IncidentStatus.VERIFIED, IncidentStatus.CLOSED);

        Incident closed = incidentService.getIncident(id);
        assertThat(closed.getRespondedAt()).isNotNull();
        assertThat(closed.getResolvedAt()).isNotNull();
        assertThat(closed.isResponseBreached()).isFalse();
        assertThat(closed.isResolutionBreached()).isFalse();
    }

    @Test
    void rejectedTransitionWritesNoAuditEntry() {
        Long id = reportWifiOutage().getId();
        int entriesBefore = auditService.trailFor(id).size();

        assertThatThrownBy(() -> incidentService.updateStatus(id, IncidentStatus.CLOSED))
                .isInstanceOf(InvalidStateTransitionException.class);

        assertThat(auditService.trailFor(id)).hasSize(entriesBefore);
        assertThat(incidentService.getIncident(id).getStatus()).isEqualTo(IncidentStatus.ASSIGNED);
    }

    @Test
    void overdueIncidentIsFlaggedAuditedAndNotifiedExactlyOnce() {
        Long id = reportWifiOutage().getId();
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

    @Test
    void reopeningRestartsTheResolutionClock() {
        Long id = reportWifiOutage().getId();
        incidentService.updateStatus(id, IncidentStatus.ACKNOWLEDGED);
        incidentService.updateStatus(id, IncidentStatus.IN_PROGRESS);
        incidentService.updateStatus(id, IncidentStatus.RESOLVED, "Restarted the access point");
        assertThat(incidentService.getIncident(id).getResolvedAt()).isNotNull();

        incidentService.updateStatus(id, IncidentStatus.IN_PROGRESS); // verification failed

        assertThat(incidentService.getIncident(id).getResolvedAt()).isNull();
    }
}
