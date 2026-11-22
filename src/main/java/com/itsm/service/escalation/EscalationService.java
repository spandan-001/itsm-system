package com.itsm.service.escalation;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itsm.enums.EscalationLevel;
import com.itsm.enums.IncidentStatus;
import com.itsm.enums.Priority;
import com.itsm.event.IncidentEscalatedEvent;
import com.itsm.model.Incident;
import com.itsm.repository.IncidentRepository;
import com.itsm.service.notification.Recipient;

/**
 * Automatic escalation (intelligent feature). A P1/P2 incident that has not
 * been acknowledged moves up Technician, Team Lead, System Administrator, IT
 * Manager as its response window is exceeded (see {@link EscalationLevel}).
 * Each step is audited and notifies that level's contact and the technician.
 */
@Service
public class EscalationService {

    private static final Pattern CONTACT = Pattern.compile("\\s*(.+?)\\s*<\\s*([^>]+?)\\s*>\\s*");
    private static final Set<IncidentStatus> UNACKNOWLEDGED =
            EnumSet.of(IncidentStatus.CLASSIFIED, IncidentStatus.QUEUED, IncidentStatus.ASSIGNED);

    private final IncidentRepository incidents;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;
    private final Set<Priority> priorities;
    private final Map<EscalationLevel, Recipient> contacts = new EnumMap<>(EscalationLevel.class);

    public EscalationService(IncidentRepository incidents, ApplicationEventPublisher eventPublisher, Clock clock,
                             @Value("${itsm.escalation.priorities:P1,P2}") Set<Priority> priorities,
                             @Value("${itsm.escalation.team-lead:Network Team Lead <team.lead@example.test>}") String teamLead,
                             @Value("${itsm.escalation.system-administrator:System Administrator <sysadmin@example.test>}") String sysadmin,
                             @Value("${itsm.escalation.it-manager:IT Manager <it.manager@example.test>}") String manager) {
        this.incidents = incidents;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
        this.priorities = Set.copyOf(priorities);
        contacts.put(EscalationLevel.TEAM_LEAD, parse(teamLead));
        contacts.put(EscalationLevel.SYSTEM_ADMINISTRATOR, parse(sysadmin));
        contacts.put(EscalationLevel.IT_MANAGER, parse(manager));
    }

    @Transactional
    public int escalate() {
        return escalate(LocalDateTime.now(clock));
    }

    /** Escalates every overdue unacknowledged incident; returns how many moved up. */
    @Transactional
    public int escalate(LocalDateTime now) {
        int count = 0;
        for (Long id : incidents.findEscalationCandidateIds(priorities, UNACKNOWLEDGED)) {
            Incident incident = incidents.findLockedById(id).orElse(null);
            if (incident == null || incident.getRespondedAt() != null) {
                continue;
            }
            EscalationLevel target = EscalationLevel.forElapsed(responseWindows(incident, now));
            EscalationLevel from = incident.getEscalationLevel();
            if (incident.escalateTo(target)) {
                eventPublisher.publishEvent(new IncidentEscalatedEvent(incident, from, target, contacts.get(target)));
                count++;
            }
        }
        return count;
    }

    /** How many response windows have passed since the incident was reported. */
    static double responseWindows(Incident incident, LocalDateTime now) {
        long window = Duration.between(incident.getCreatedAt(), incident.getResponseDueAt()).toSeconds();
        long elapsed = Duration.between(incident.getCreatedAt(), now).toSeconds();
        return window <= 0 ? 0 : elapsed / (double) window;
    }

    public List<Recipient> chain() {
        return List.copyOf(contacts.values());
    }

    static Recipient parse(String contact) {
        Matcher m = CONTACT.matcher(contact);
        if (!m.matches()) {
            throw new IllegalArgumentException("Escalation contact must look like 'Name <email>': " + contact);
        }
        return new Recipient(m.group(1), m.group(2));
    }
}
