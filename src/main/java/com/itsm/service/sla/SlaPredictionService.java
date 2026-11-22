package com.itsm.service.sla;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itsm.enums.SlaRisk;
import com.itsm.model.Incident;
import com.itsm.repository.IncidentRepository;
import com.itsm.service.routing.RoutingService;
import com.itsm.state.IncidentStates;

/**
 * Predictive SLA risk (intelligent feature). Instead of waiting for 75% of the
 * window to pass, estimate when each open incident will really be resolved:
 *
 * <ol>
 *   <li>Baseline: the median time past incidents took to resolve, using the
 *       most specific history with at least {@value #MIN_SAMPLES} examples:
 *       same category and priority, then same priority, then same category.
 *       With no history, half of the SLA resolution target.</li>
 *   <li>Workload: +25% for each other open incident the assigned technician
 *       has, or +50% while nobody is assigned.</li>
 *   <li>An incident already running late is predicted to need at least another
 *       10% of its baseline.</li>
 * </ol>
 *
 * Risk is HIGH when the predicted finish is after the deadline, MEDIUM when it
 * is in the last 20% of the window, otherwise LOW.
 */
@Service
public class SlaPredictionService {

    static final int MIN_SAMPLES = 3;
    static final double PER_OTHER_INCIDENT = 0.25;
    static final double UNASSIGNED_FACTOR = 1.5;

    private final IncidentRepository incidents;
    private final RoutingService routing;
    private final Clock clock;

    public SlaPredictionService(IncidentRepository incidents, RoutingService routing, Clock clock) {
        this.incidents = incidents;
        this.routing = routing;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public SlaPrediction predict(Incident incident) {
        return predict(incident, LocalDateTime.now(clock), history(), routing.activeWorkload());
    }

    /** Open incidents predicted to miss their resolution deadline, tightest first. */
    @Transactional(readOnly = true)
    public List<SlaPrediction> atRisk() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Incident> history = history();
        Map<Long, Long> workload = routing.activeWorkload();
        return incidents.findAll().stream()
                .filter(i -> i.getResolvedAt() == null && i.getResolutionDueAt() != null && !i.isDuplicate()
                        && !IncidentStates.closedStatuses().contains(i.getStatus()))
                .map(i -> predict(i, now, history, workload))
                .filter(p -> p.risk() == SlaRisk.HIGH)
                .sorted(Comparator.comparingLong(SlaPrediction::marginMinutes))
                .toList();
    }

    private List<Incident> history() {
        return incidents.findAll().stream().filter(i -> i.getResolvedAt() != null && !i.isDuplicate()).toList();
    }

    private SlaPrediction predict(Incident incident, LocalDateTime now, List<Incident> history, Map<Long, Long> workload) {
        int others = 0;
        if (incident.getAssignedTechnician() != null) {
            long active = workload.getOrDefault(incident.getAssignedTechnician().getId(), 0L);
            boolean countsItself = IncidentStates.of(incident.getStatus()).isActiveWork();
            others = (int) Math.max(0, active - (countsItself ? 1 : 0));
        }
        return predict(incident, now, history, others);
    }

    /** The pure calculation, separated so it can be tested with exact numbers. */
    static SlaPrediction predict(Incident incident, LocalDateTime now, List<Incident> history, int otherActiveIncidents) {
        LocalDateTime due = incident.getResolutionDueAt();
        if (incident.getResolvedAt() != null || due == null) {
            return new SlaPrediction(incident.getId(), incident.getTitle(), incident.getResolvedAt(), due,
                    SlaRisk.NOT_APPLICABLE, null, incident.getResolvedAt() != null ? "already resolved" : "no SLA",
                    0, 1.0);
        }
        Duration window = Duration.between(incident.getCreatedAt(), due);

        Baseline baseline = baseline(incident, history, window);
        double factor = incident.getAssignedTechnician() == null
                ? UNASSIGNED_FACTOR
                : 1 + PER_OTHER_INCIDENT * otherActiveIncidents;
        Duration expected = scale(baseline.duration(), factor);

        LocalDateTime predicted = incident.getCreatedAt().plus(expected);
        LocalDateTime earliest = now.plus(scale(baseline.duration(), 0.1));
        if (predicted.isBefore(earliest)) {
            predicted = earliest; // already running late: it still needs some time
        }

        SlaRisk risk;
        if (predicted.isAfter(due)) {
            risk = SlaRisk.HIGH;
        } else if (predicted.isAfter(incident.getCreatedAt().plus(scale(window, 0.8)))) {
            risk = SlaRisk.MEDIUM;
        } else {
            risk = SlaRisk.LOW;
        }
        return new SlaPrediction(incident.getId(), incident.getTitle(), predicted, due, risk,
                Duration.between(predicted, due).toMinutes(), baseline.basis(), baseline.samples(),
                Math.round(factor * 100) / 100.0);
    }

    private static Baseline baseline(Incident incident, List<Incident> history, Duration window) {
        List<Rule> rules = List.of(
                new Rule("same category and priority",
                        i -> i.getCategory() == incident.getCategory() && i.getPriority() == incident.getPriority()),
                new Rule("same priority", i -> i.getPriority() == incident.getPriority()),
                new Rule("same category", i -> i.getCategory() == incident.getCategory()));
        for (Rule rule : rules) {
            List<Duration> durations = history.stream()
                    .filter(i -> i != incident && (i.getId() == null || !i.getId().equals(incident.getId())))
                    .filter(rule.matches())
                    .map(i -> Duration.between(i.getCreatedAt(), i.getResolvedAt()))
                    .sorted()
                    .toList();
            if (durations.size() >= MIN_SAMPLES) {
                return new Baseline(median(durations), "median of " + durations.size() + " past incidents ("
                        + rule.name() + ")", durations.size());
            }
        }
        return new Baseline(scale(window, 0.5), "no history yet: half the SLA target", 0);
    }

    private static Duration median(List<Duration> sorted) {
        int n = sorted.size();
        return n % 2 == 1 ? sorted.get(n / 2) : sorted.get(n / 2 - 1).plus(sorted.get(n / 2)).dividedBy(2);
    }

    private static Duration scale(Duration duration, double factor) {
        return Duration.ofSeconds(Math.round(duration.toSeconds() * factor));
    }

    private record Rule(String name, Predicate<Incident> matches) {
    }

    private record Baseline(Duration duration, String basis, int samples) {
    }
}
