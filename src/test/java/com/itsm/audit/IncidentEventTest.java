package com.itsm.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.itsm.TestData;
import com.itsm.enums.AuditAction;
import com.itsm.enums.IncidentStatus;
import com.itsm.enums.Priority;
import com.itsm.enums.SlaType;
import com.itsm.event.IncidentAssignedEvent;
import com.itsm.event.IncidentCreatedEvent;
import com.itsm.event.IncidentEvent;
import com.itsm.event.IncidentStatusChangedEvent;
import com.itsm.event.SlaBreachedEvent;
import com.itsm.model.Incident;
import com.itsm.model.Technician;

class IncidentEventTest {

    @Test
    void eachEventTypeMapsToItsOwnAuditAction() {
        Incident incident = TestData.incident(Priority.P2);
        Technician tech = new Technician("Ravi", "ravi@example.test", "NETWORK", true);

        List<IncidentEvent> events = List.of(
                new IncidentCreatedEvent(incident, null),
                new IncidentAssignedEvent(incident, IncidentStatus.CLASSIFIED, tech, "admin"),
                new IncidentStatusChangedEvent(incident, IncidentStatus.ASSIGNED, IncidentStatus.ACKNOWLEDGED, "ravi"),
                new SlaBreachedEvent(incident, SlaType.RESOLUTION, incident.getCreatedAt()));

        assertThat(events).extracting(IncidentEvent::getAuditAction).containsExactly(
                AuditAction.CREATED, AuditAction.ASSIGNED, AuditAction.STATUS_CHANGED, AuditAction.SLA_BREACHED);
        assertThat(events).allSatisfy(e -> assertThat(e.describe()).isNotBlank());
    }

    @Test
    void eventsCarryTransitionAndDefaultActor() {
        Incident incident = TestData.incident(Priority.P2);
        IncidentStatusChangedEvent changed =
                new IncidentStatusChangedEvent(incident, IncidentStatus.NEW, IncidentStatus.CLASSIFIED, " ");

        assertThat(changed.getFromStatus()).isEqualTo(IncidentStatus.NEW);
        assertThat(changed.getToStatus()).isEqualTo(IncidentStatus.CLASSIFIED);
        assertThat(changed.getActor()).isEqualTo(IncidentEvent.SYSTEM_ACTOR);
        assertThat(changed.describe()).isEqualTo("Status changed from NEW to CLASSIFIED");
    }
}
