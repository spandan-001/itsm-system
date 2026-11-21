package com.itsm.sla;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.itsm.TestData;
import com.itsm.enums.IncidentStatus;
import com.itsm.enums.Priority;
import com.itsm.enums.SlaState;
import com.itsm.enums.SlaType;
import com.itsm.event.SlaBreachedEvent;
import com.itsm.model.Incident;
import com.itsm.model.SlaPolicy;
import com.itsm.repository.IncidentRepository;
import com.itsm.repository.SlaPolicyRepository;
import com.itsm.service.sla.SlaService;
import com.itsm.service.sla.SlaSnapshot;

class SlaServiceTest {

    private SlaPolicyRepository policyRepository;
    private IncidentRepository incidentRepository;
    private final List<Object> published = new ArrayList<>();

    @BeforeEach
    void setUp() {
        policyRepository = mock(SlaPolicyRepository.class);
        incidentRepository = mock(IncidentRepository.class);
        when(policyRepository.findByPriority(any())).thenReturn(Optional.empty());
    }

    /** A service whose clock reads the given time. */
    private SlaService serviceAt(LocalDateTime now) {
        ZoneId zone = ZoneId.systemDefault();
        Clock clock = Clock.fixed(now.atZone(zone).toInstant(), zone);
        return new SlaService(policyRepository, incidentRepository, published::add, clock);
    }

    @Test
    void appliesDefaultDeadlinesFromCreationTime() {
        Incident incident = TestData.incident(Priority.P1);
        serviceAt(LocalDateTime.now()).applyDeadlines(incident);

        assertThat(incident.getResponseDueAt()).isEqualTo(incident.getCreatedAt().plusMinutes(15));
        assertThat(incident.getResolutionDueAt()).isEqualTo(incident.getCreatedAt().plusHours(4));
    }

    @Test
    void storedPolicyOverridesDefault() {
        when(policyRepository.findByPriority(Priority.P3)).thenReturn(Optional.of(new SlaPolicy(Priority.P3, 30, 90)));
        Incident incident = TestData.incident(Priority.P3);
        serviceAt(LocalDateTime.now()).applyDeadlines(incident);

        assertThat(incident.getResolutionDueAt()).isEqualTo(incident.getCreatedAt().plusMinutes(90));
    }

    @Test
    void deadlinesNeedPriorityFirst() {
        Incident incident = TestData.incident(null);
        assertThatThrownBy(() -> serviceAt(LocalDateTime.now()).applyDeadlines(incident))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void evaluatesOnTrackAtRiskAndBreached() {
        Incident incident = TestData.incident(Priority.P1); // resolution window 4h
        SlaService service = serviceAt(LocalDateTime.now());
        service.applyDeadlines(incident);
        LocalDateTime start = incident.getCreatedAt();

        assertThat(service.evaluate(incident, start.plusHours(1)).resolutionState()).isEqualTo(SlaState.ON_TRACK);
        assertThat(service.evaluate(incident, start.plusHours(3).plusMinutes(30)).resolutionState()).isEqualTo(SlaState.AT_RISK);
        SlaSnapshot late = service.evaluate(incident, start.plusHours(5));
        assertThat(late.resolutionState()).isEqualTo(SlaState.BREACHED);
        assertThat(late.minutesUntilResolutionDue()).isEqualTo(-60);
    }

    @Test
    void onTimeAcknowledgementMeetsResponseSla() {
        Incident incident = TestData.incident(Priority.P2);
        SlaService setup = serviceAt(LocalDateTime.now());
        setup.applyDeadlines(incident);

        serviceAt(incident.getCreatedAt().plusMinutes(10)).onStatusChange(incident, IncidentStatus.ACKNOWLEDGED);

        assertThat(incident.getRespondedAt()).isEqualTo(incident.getCreatedAt().plusMinutes(10));
        assertThat(incident.isResponseBreached()).isFalse();
        assertThat(published).isEmpty();
        assertThat(setup.evaluate(incident).responseState()).isEqualTo(SlaState.MET);
    }

    @Test
    void lateAcknowledgementPublishesOneBreachEvent() {
        Incident incident = TestData.incident(Priority.P1);
        serviceAt(LocalDateTime.now()).applyDeadlines(incident);
        SlaService late = serviceAt(incident.getCreatedAt().plusMinutes(20));

        late.onStatusChange(incident, IncidentStatus.ACKNOWLEDGED);
        late.onStatusChange(incident, IncidentStatus.ACKNOWLEDGED); // clock already stopped

        assertThat(incident.isResponseBreached()).isTrue();
        assertThat(published).hasSize(1).first()
                .isInstanceOfSatisfying(SlaBreachedEvent.class,
                        e -> assertThat(e.getSlaType()).isEqualTo(SlaType.RESPONSE));
    }

    @Test
    void detectBreachesFlagsOverdueIncidents() {
        Incident overdue = TestData.incident(Priority.P1);
        LocalDateTime now = LocalDateTime.now().plusHours(5);
        SlaService service = serviceAt(now);
        service.applyDeadlines(overdue);
        when(incidentRepository.findOverdueIds(now)).thenReturn(List.of(1L));
        when(incidentRepository.findLockedById(1L)).thenReturn(Optional.of(overdue));

        int count = service.detectBreaches(now);

        assertThat(service.detectBreaches(now)).isZero();
        assertThat(count).isEqualTo(2);
        assertThat(overdue.isResponseBreached()).isTrue();
        assertThat(overdue.isResolutionBreached()).isTrue();
        assertThat(published).hasSize(2).allMatch(e -> e instanceof SlaBreachedEvent);
    }
}
