package com.itsm;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.itsm.enums.AuditAction;
import com.itsm.enums.EscalationLevel;
import com.itsm.enums.Impact;
import com.itsm.enums.IncidentCategory;
import com.itsm.enums.IncidentStatus;
import com.itsm.enums.Urgency;
import com.itsm.model.Incident;
import com.itsm.model.Notification;
import com.itsm.service.audit.AuditService;
import com.itsm.service.escalation.EscalationService;
import com.itsm.service.notification.NotificationService;

class EscalationIntegrationTest extends IntegrationTestSupport {

    @Autowired EscalationService escalationService;
    @Autowired AuditService auditService;
    @Autowired NotificationService notificationService;

    @Test
    void levelsFollowTheResponseWindow() {
        assertThat(EscalationLevel.forElapsed(0.5)).isEqualTo(EscalationLevel.NONE);
        assertThat(EscalationLevel.forElapsed(1.0)).isEqualTo(EscalationLevel.TEAM_LEAD);
        assertThat(EscalationLevel.forElapsed(1.7)).isEqualTo(EscalationLevel.SYSTEM_ADMINISTRATOR);
        assertThat(EscalationLevel.forElapsed(9)).isEqualTo(EscalationLevel.IT_MANAGER);
    }

    @Test
    void unacknowledgedP1ClimbsTheChainOneStepAtATime() {
        Incident p1 = reportWifiOutage(); // P1: response due in 15 minutes, auto-assigned
        var start = p1.getCreatedAt();

        assertThat(escalationService.escalate(start.plusMinutes(10))).isZero();
        assertThat(escalationService.escalate(start.plusMinutes(16))).isEqualTo(1);
        assertThat(escalationService.escalate(start.plusMinutes(17))).isZero(); // already at team lead
        assertThat(escalationService.escalate(start.plusMinutes(23))).isEqualTo(1);
        assertThat(escalationService.escalate(start.plusMinutes(31))).isEqualTo(1);
        assertThat(escalationService.escalate(start.plusHours(5))).isZero(); // top of the chain

        assertThat(incidentService.getIncident(p1.getId()).getEscalationLevel()).isEqualTo(EscalationLevel.IT_MANAGER);
        assertThat(auditService.trailFor(p1.getId())).filteredOn(l -> l.getAction() == AuditAction.ESCALATED)
                .hasSize(3);
        assertThat(notificationService.forRecipient("team.lead@example.test")).extracting(Notification::getSubject)
                .anyMatch(s -> s.contains("escalated to TEAM_LEAD"));
        assertThat(notificationService.forRecipient("it.manager@example.test")).isNotEmpty();
        assertThat(notificationService.forRecipient("ravi@example.test")).extracting(Notification::getSubject)
                .anyMatch(s -> s.contains("escalated"));
    }

    @Test
    void acknowledgedAndLowPriorityIncidentsAreNotEscalated() {
        Incident p1 = reportWifiOutage();
        incidentService.updateStatus(p1.getId(), IncidentStatus.ACKNOWLEDGED);
        Incident p4 = report("Printer offline", "Second floor printer is offline", IncidentCategory.HARDWARE,
                Impact.LOW, Urgency.LOW);

        assertThat(escalationService.escalate(p1.getCreatedAt().plusDays(5))).isZero();
        assertThat(incidentService.getIncident(p4.getId()).getEscalationLevel()).isEqualTo(EscalationLevel.NONE);
    }
}
