package com.itsm.service.sla;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.itsm.TestData;
import com.itsm.enums.IncidentCategory;
import com.itsm.enums.IncidentStatus;
import com.itsm.enums.Priority;
import com.itsm.enums.SlaRisk;
import com.itsm.model.Incident;
import com.itsm.model.Technician;

/** The prediction maths with exact numbers (P1: resolution due 4 hours after creation). */
class SlaPredictionTest {

    private final Technician tech = new Technician("Ravi", "ravi@example.test", "NETWORK", true);

    private Incident openP1(boolean assigned) {
        Incident incident = TestData.incident(Priority.P1);
        incident.applySlaDeadlines(incident.getCreatedAt().plusMinutes(15), incident.getCreatedAt().plusHours(4));
        incident.transitionTo(IncidentStatus.CLASSIFIED);
        if (assigned) {
            incident.assignTo(tech);
        }
        return incident;
    }

    /** Past P1 network incidents that each took the given number of hours. */
    private static List<Incident> history(double... hours) {
        return java.util.Arrays.stream(hours).mapToObj(h -> {
            Incident past = TestData.incident(Priority.P1);
            past.recordResolution(past.getCreatedAt().plusMinutes(Math.round(h * 60)));
            return past;
        }).toList();
    }

    @Test
    void withoutHistoryItAssumesHalfTheTarget() {
        Incident incident = openP1(true);
        SlaPrediction p = SlaPredictionService.predict(incident, incident.getCreatedAt(), List.of(), 0);

        assertThat(p.predictedResolutionAt()).isEqualTo(incident.getCreatedAt().plusHours(2));
        assertThat(p.risk()).isEqualTo(SlaRisk.LOW);
        assertThat(p.sampleSize()).isZero();
        assertThat(p.basis()).contains("no history");
    }

    @Test
    void similarPastIncidentsThatTookLongerThanTheTargetMeanHighRisk() {
        Incident incident = openP1(true);
        SlaPrediction p = SlaPredictionService.predict(incident, incident.getCreatedAt(), history(4.5, 5, 6), 0);

        assertThat(p.predictedResolutionAt()).isEqualTo(incident.getCreatedAt().plusHours(5)); // median
        assertThat(p.risk()).isEqualTo(SlaRisk.HIGH);
        assertThat(p.marginMinutes()).isEqualTo(-60);
        assertThat(p.basis()).contains("same category and priority");
    }

    @Test
    void aBusyTechnicianPushesTheEstimateOutward() {
        Incident incident = openP1(true);
        var history = history(3, 3, 3);

        assertThat(SlaPredictionService.predict(incident, incident.getCreatedAt(), history, 0).risk())
                .isEqualTo(SlaRisk.LOW);    // 3 h, well inside 4 h
        SlaPrediction busy = SlaPredictionService.predict(incident, incident.getCreatedAt(), history, 2);
        assertThat(busy.workloadFactor()).isEqualTo(1.5);
        assertThat(busy.risk()).isEqualTo(SlaRisk.HIGH); // 4.5 h
    }

    @Test
    void unassignedIncidentsWaitLongerAndLateOnesStillNeedTime() {
        Incident queued = openP1(false);
        var history = history(2, 2, 2);
        assertThat(SlaPredictionService.predict(queued, queued.getCreatedAt(), history, 0).risk())
                .isEqualTo(SlaRisk.LOW); // 3 h
        assertThat(SlaPredictionService.predict(queued, queued.getCreatedAt(), history(2.5, 2.5, 2.5), 0).risk())
                .isEqualTo(SlaRisk.MEDIUM); // 3.75 h: inside the last 20% of the window

        Incident late = openP1(true);
        var now = late.getCreatedAt().plusHours(3).plusMinutes(50);
        SlaPrediction p = SlaPredictionService.predict(late, now, history, 0);
        assertThat(p.predictedResolutionAt()).isAfterOrEqualTo(now.plusMinutes(12)); // +10% of 2 h
        assertThat(p.risk()).isEqualTo(SlaRisk.HIGH);
    }

    @Test
    void fallsBackToBroaderHistoryAndIgnoresResolvedIncidents() {
        Incident incident = openP1(true);
        // Only two same-category P1s, but three P1s overall once a hardware one is added.
        Incident hardware = new Incident("Laptop dead", "Will not boot", IncidentCategory.HARDWARE,
                com.itsm.enums.Impact.HIGH, com.itsm.enums.Urgency.HIGH, TestData.employee(), List.of());
        hardware.setPriority(Priority.P1);
        hardware.recordResolution(hardware.getCreatedAt().plusHours(1));
        var mixed = new java.util.ArrayList<>(history(1, 1));
        mixed.add(hardware);

        assertThat(SlaPredictionService.predict(incident, incident.getCreatedAt(), mixed, 0).basis())
                .contains("same priority");

        Incident resolved = openP1(true);
        resolved.recordResolution(resolved.getCreatedAt().plusHours(1));
        assertThat(SlaPredictionService.predict(resolved, resolved.getCreatedAt(), mixed, 0).risk())
                .isEqualTo(SlaRisk.NOT_APPLICABLE);
    }
}
