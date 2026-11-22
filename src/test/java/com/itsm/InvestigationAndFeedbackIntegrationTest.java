package com.itsm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import com.itsm.enums.AuditAction;
import com.itsm.enums.Impact;
import com.itsm.enums.IncidentCategory;
import com.itsm.enums.IncidentStatus;
import com.itsm.enums.NoteType;
import com.itsm.enums.Urgency;
import com.itsm.exception.BusinessRuleException;
import com.itsm.exception.InvalidStateTransitionException;
import com.itsm.exception.OperationNotPermittedException;
import com.itsm.exception.TechnicianWorkloadExceededException;
import com.itsm.model.AccountLink.Role;
import com.itsm.model.AuditLog;
import com.itsm.model.Incident;
import com.itsm.model.IncidentNote;
import com.itsm.model.Notification;
import com.itsm.service.AccountService.LinkedAccount;
import com.itsm.service.analytics.AnalyticsService;
import com.itsm.service.audit.AuditService;
import com.itsm.service.note.NoteService;
import com.itsm.service.notification.NotificationService;
import com.itsm.service.search.IncidentSearchCriteria;
import com.itsm.service.search.IncidentSearchService;
import com.itsm.service.sla.SlaService;
import com.itsm.strategy.assignment.TechnicianCandidate;

/** Investigation thread, resolution details, reopening, feedback, SLA warnings and role rules. */
class InvestigationAndFeedbackIntegrationTest extends IntegrationTestSupport {

    @Autowired NoteService noteService;
    @Autowired AuditService auditService;
    @Autowired NotificationService notificationService;
    @Autowired SlaService slaService;
    @Autowired AnalyticsService analyticsService;
    @Autowired IncidentSearchService searchService;

    LinkedAccount employee;
    LinkedAccount technician;
    final LinkedAccount admin = new LinkedAccount(Role.ADMIN, null, "admin@example.test");

    @BeforeEach
    void accounts() {
        employee = new LinkedAccount(Role.EMPLOYEE, reporter.getId(), reporter.getEmail());
        technician = new LinkedAccount(Role.TECHNICIAN, networkTech.getId(), networkTech.getEmail());
    }

    private Long inProgressIncident() {
        Long id = reportWifiOutage().getId(); // auto-assigned to Ravi
        incidentService.updateStatus(id, IncidentStatus.ACKNOWLEDGED, technician);
        incidentService.updateStatus(id, IncidentStatus.IN_PROGRESS, technician);
        return id;
    }

    private List<String> subjectsFor(String email) {
        return notificationService.forRecipient(email).stream().map(Notification::getSubject).toList();
    }

    @Test
    void technicianRequestsInformationAndReporterReplies() {
        Long id = inProgressIncident();

        noteService.addNote(id, NoteType.INFO_REQUEST, "Which floor are you on?", technician);
        assertThat(incidentService.getIncident(id).isAwaitingReporterInfo()).isTrue();
        assertThat(subjectsFor("asha@example.test")).anyMatch(s -> s.contains("more information needed"));

        noteService.addNote(id, NoteType.INFO_RESPONSE, "Second floor, reading room", employee);
        assertThat(incidentService.getIncident(id).isAwaitingReporterInfo()).isFalse();
        assertThat(subjectsFor("ravi@example.test")).anyMatch(s -> s.contains("reporter added information"));

        assertThat(noteService.notesFor(id, employee)).extracting(IncidentNote::getAuthorName)
                .containsExactly("Ravi Kumar", "Asha Rao");
        assertThat(auditService.trailFor(id)).filteredOn(l -> l.getAction() == AuditAction.NOTE_ADDED)
                .extracting(AuditLog::getDetails)
                .containsExactly("Information requested by Ravi Kumar (TECHNICIAN): Which floor are you on?",
                        "Information provided by Asha Rao (EMPLOYEE): Second floor, reading room");
    }

    @Test
    void investigationNotesStayInternal() {
        Long id = inProgressIncident();
        noteService.addNote(id, NoteType.INVESTIGATION, "AP-3 firmware crashed, checking logs", technician);

        assertThat(noteService.notesFor(id, technician)).hasSize(1);
        assertThat(noteService.notesFor(id, admin)).hasSize(1);
        assertThat(noteService.notesFor(id, employee)).isEmpty();
        assertThat(auditService.trailFor(id)).extracting(AuditLog::getDetails)
                .noneMatch(d -> d.contains("firmware"));
    }

    @Test
    void eachRoleMayPostOnlyItsOwnNoteTypes() {
        Long id = inProgressIncident();

        assertThatThrownBy(() -> noteService.addNote(id, NoteType.INVESTIGATION, "x", employee))
                .isInstanceOf(OperationNotPermittedException.class);
        assertThatThrownBy(() -> noteService.addNote(id, NoteType.ADDITIONAL_INFO, "x", technician))
                .isInstanceOf(OperationNotPermittedException.class);
        assertThatThrownBy(() -> noteService.addNote(id, NoteType.RESOLUTION, "x", technician))
                .isInstanceOf(OperationNotPermittedException.class); // written by the system only
        assertThat(NoteType.INFO_REQUEST.canBeWrittenBy(Role.ADMIN)).isTrue();
    }

    @Test
    void resolvingRequiresASummaryThatTheReporterSees() {
        Long id = inProgressIncident();

        assertThatThrownBy(() -> incidentService.updateStatus(id, IncidentStatus.RESOLVED, technician))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("resolution summary is required");

        incidentService.updateStatus(id, IncidentStatus.RESOLVED, "Rebooted access point AP-3", technician);

        Incident resolved = incidentService.getIncident(id);
        assertThat(resolved.getResolutionSummary()).isEqualTo("Rebooted access point AP-3");
        assertThat(resolved.getResolvedBy()).isEqualTo("ravi@example.test");
        assertThat(noteService.notesFor(id, employee)).extracting(IncidentNote::getType)
                .containsExactly(NoteType.RESOLUTION);
        assertThat(notificationService.forRecipient("asha@example.test"))
                .anyMatch(n -> n.getMessage().contains("Resolution: Rebooted access point AP-3"));
    }

    @Test
    void reporterCanReopenWithAReasonButNotDoTechnicianSteps() {
        Long id = inProgressIncident();
        incidentService.updateStatus(id, IncidentStatus.RESOLVED, "Rebooted access point AP-3", technician);

        incidentService.updateStatus(id, IncidentStatus.IN_PROGRESS, "Still dropping every few minutes", employee);

        Incident reopened = incidentService.getIncident(id);
        assertThat(reopened.getStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);
        assertThat(reopened.getResolutionSummary()).isNull();
        assertThat(noteService.notesFor(id, technician)).extracting(IncidentNote::getType)
                .containsExactly(NoteType.RESOLUTION, NoteType.REOPEN_REASON);
        assertThat(subjectsFor("ravi@example.test")).anyMatch(s -> s.endsWith("reopened"));
        assertThatThrownBy(() -> incidentService.updateStatus(id, IncidentStatus.RESOLVED, "done", employee))
                .isInstanceOf(OperationNotPermittedException.class);
    }

    @Test
    void reporterRatesTheResolutionOnce() {
        Long id = inProgressIncident();
        assertThatThrownBy(() -> incidentService.recordFeedback(id, 5, null, employee))
                .isInstanceOf(BusinessRuleException.class); // not resolved yet
        incidentService.updateStatus(id, IncidentStatus.RESOLVED, "Rebooted access point AP-3", technician);

        assertThatThrownBy(() -> incidentService.recordFeedback(id, 6, null, employee))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> incidentService.recordFeedback(id, 4, null, technician))
                .isInstanceOf(OperationNotPermittedException.class);

        incidentService.recordFeedback(id, 4, "Quick fix, thanks", employee);

        assertThatThrownBy(() -> incidentService.recordFeedback(id, 5, null, employee))
                .isInstanceOf(BusinessRuleException.class); // only once
        assertThat(incidentService.getIncident(id).getFeedbackRating()).isEqualTo(4);
        assertThat(auditService.trailFor(id)).extracting(AuditLog::getAction).contains(AuditAction.FEEDBACK_RECEIVED);
        assertThat(analyticsService.summary().averageFeedbackRating()).isEqualTo(4.0);
        assertThat(subjectsFor("ravi@example.test")).anyMatch(s -> s.contains("feedback: 4/5"));
    }

    @Test
    void slaWarningIsRaisedOncePerClockBeforeTheDeadline() {
        Incident incident = reportWifiOutage(); // P1: respond in 15 min, resolve in 4 h
        var start = incident.getCreatedAt();

        assertThat(slaService.detectWarnings(start.plusMinutes(5))).isZero();   // 33% used
        assertThat(slaService.detectWarnings(start.plusMinutes(12))).isEqualTo(1); // response 80% used
        assertThat(slaService.detectWarnings(start.plusMinutes(13))).isZero();  // already warned
        assertThat(slaService.detectWarnings(start.plusHours(3).plusMinutes(30))).isEqualTo(1); // resolution

        assertThat(auditService.trailFor(incident.getId()))
                .filteredOn(l -> l.getAction() == AuditAction.SLA_WARNING).hasSize(2);
        assertThat(subjectsFor("ravi@example.test")).filteredOn(s -> s.contains("SLA at risk")).hasSize(4); // 2 x 2 channels
    }

    @Test
    void manualAssignmentRespectsTheWorkloadLimit() {
        String[][] distinct = {
                {"Router reboot loop", "Core router restarts every hour"},
                {"VPN certificate expired", "Remote staff rejected at login"},
                {"DNS lookups failing", "Internal names do not resolve"},
                {"Switch port flapping", "Lab bench port goes up and down"},
                {"Firewall blocks scanner", "Network scanner traffic dropped"},
        };
        for (String[] report : distinct) {
            report(report[0], report[1], IncidentCategory.NETWORK, Impact.LOW, Urgency.LOW);
        }
        assertThat(incidentRepository.findAll()).allMatch(i -> i.getAssignedTechnician() != null);

        Incident overflow = report("Proxy timeout errors", "Outbound proxy times out on uploads",
                IncidentCategory.NETWORK, Impact.LOW, Urgency.LOW);
        assertThat(overflow.getStatus()).isEqualTo(IncidentStatus.QUEUED); // Ravi is at capacity

        assertThatThrownBy(() -> incidentService.assignTechnician(overflow.getId(), networkTech.getId()))
                .isInstanceOf(TechnicianWorkloadExceededException.class)
                .hasMessageContaining("limit " + TechnicianCandidate.MAX_ACTIVE_INCIDENTS);
    }

    @Test
    void adminSeesEveryIncidentAndCanDriveTheWorkflow() {
        Long id = reportWifiOutage().getId();

        assertThat(incidentService.getIncident(id, admin).getId()).isEqualTo(id);
        assertThat(searchService.search(IncidentSearchCriteria.none(), PageRequest.of(0, 10), admin)
                .getTotalElements()).isEqualTo(1);
        assertThat(searchService.search(IncidentSearchCriteria.none(), PageRequest.of(0, 10),
                new LinkedAccount(Role.EMPLOYEE, -1L, "nobody@example.test")).getTotalElements()).isZero();

        incidentService.updateStatus(id, IncidentStatus.ACKNOWLEDGED, admin);
        assertThat(incidentService.getIncident(id).getStatus()).isEqualTo(IncidentStatus.ACKNOWLEDGED);
    }

    @Test
    void searchByIncidentIdAndAwaitingInformation() {
        Long first = inProgressIncident();
        report("Printer offline", "Second floor printer is offline", IncidentCategory.HARDWARE, Impact.LOW, Urgency.LOW);
        noteService.addNote(first, NoteType.INFO_REQUEST, "Which floor?", technician);

        var byId = new IncidentSearchCriteria(null, null, null, null, null, null, null, null, null, null, null, null,
                first, null);
        var awaiting = new IncidentSearchCriteria(null, null, null, null, null, null, null, null, null, null, null, null,
                null, true);

        assertThat(searchService.search(byId, PageRequest.of(0, 10)).getContent())
                .extracting(Incident::getId).containsExactly(first);
        assertThat(searchService.search(awaiting, PageRequest.of(0, 10)).getContent())
                .extracting(Incident::getId).containsExactly(first);
    }

    @Test
    void analyticsReportsIncidentsPerServiceAndCriticalOpen() {
        reportWifiOutage();
        report("Printer offline", "Second floor printer is offline", IncidentCategory.HARDWARE, Impact.LOW, Urgency.LOW);

        var summary = analyticsService.summary();

        assertThat(summary.byService()).containsEntry("Campus Wi-Fi", 2L);
        assertThat(summary.criticalOpenIncidents()).isEqualTo(1);
        assertThat(summary.averageFeedbackRating()).isNull();
    }

    @Test
    void closedIncidentsAcceptNoMoreNotes() {
        Long id = inProgressIncident();
        incidentService.updateStatus(id, IncidentStatus.RESOLVED, "Rebooted access point AP-3", technician);
        incidentService.updateStatus(id, IncidentStatus.VERIFIED, employee);
        incidentService.updateStatus(id, IncidentStatus.CLOSED, employee);

        assertThat(incidentService.getIncident(id).getClosedAt()).isNotNull();
        assertThatThrownBy(() -> noteService.addNote(id, NoteType.ADDITIONAL_INFO, "One more thing", employee))
                .isInstanceOf(BusinessRuleException.class);
    }
}
