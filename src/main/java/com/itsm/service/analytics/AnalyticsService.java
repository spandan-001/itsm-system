package com.itsm.service.analytics;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itsm.enums.IncidentCategory;
import com.itsm.enums.IncidentStatus;
import com.itsm.enums.Priority;
import com.itsm.model.Incident;
import com.itsm.repository.IncidentRepository;
import com.itsm.repository.TechnicianRepository;
import com.itsm.state.IncidentStates;

/**
 * Operational analytics, computed with streams over all incidents. Fine for
 * a course-sized dataset; a large system would push this into SQL.
 */
@Service
public class AnalyticsService {

    static final int TREND_DAYS = 7;

    private final IncidentRepository incidentRepository;
    private final TechnicianRepository technicianRepository;
    private final com.itsm.repository.ServiceOutageRepository outageRepository;
    private final com.itsm.service.sla.SlaPredictionService predictions;
    private final Clock clock;

    public AnalyticsService(IncidentRepository incidentRepository, TechnicianRepository technicianRepository,
                            com.itsm.repository.ServiceOutageRepository outageRepository,
                            com.itsm.service.sla.SlaPredictionService predictions, Clock clock) {
        this.predictions = predictions;
        this.incidentRepository = incidentRepository;
        this.technicianRepository = technicianRepository;
        this.outageRepository = outageRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AnalyticsSummary summary() {
        List<Incident> all = incidentRepository.findAll();
        List<Incident> resolved = all.stream().filter(i -> i.getResolvedAt() != null).toList();

        long open = all.stream()
                .filter(i -> !IncidentStates.closedStatuses().contains(i.getStatus()) && !i.isDuplicate())
                .count();
        long breached = all.stream().filter(i -> i.isResponseBreached() || i.isResolutionBreached()).count();
        Double compliance = resolved.isEmpty() ? null
                : round(100.0 * resolved.stream().filter(i -> !i.isResolutionBreached()).count() / resolved.size());

        Map<Priority, Double> mttrByPriority = new EnumMap<>(Priority.class);
        resolved.stream()
                .collect(Collectors.groupingBy(Incident::getPriority,
                        Collectors.averagingLong(AnalyticsService::resolutionMinutes)))
                .forEach((p, avg) -> mttrByPriority.put(p, round(avg)));

        LocalDate today = LocalDate.now(clock);
        Map<LocalDate, Long> perDay = new TreeMap<>();
        for (int d = TREND_DAYS - 1; d >= 0; d--) {
            perDay.put(today.minusDays(d), 0L);
        }
        all.forEach(i -> perDay.computeIfPresent(i.getCreatedAt().toLocalDate(), (day, n) -> n + 1));

        long criticalOpen = all.stream()
                .filter(i -> i.getPriority() == Priority.P1 && !i.isDuplicate()
                        && !IncidentStates.closedStatuses().contains(i.getStatus()))
                .count();
        var ratings = all.stream().filter(i -> i.getFeedbackRating() != null)
                .mapToInt(Incident::getFeedbackRating).summaryStatistics();

        return new AnalyticsSummary(
                all.size(),
                open,
                all.stream().filter(i -> i.getStatus() == IncidentStatus.QUEUED).count(),
                all.stream().filter(Incident::isDuplicate).count(),
                breached,
                criticalOpen,
                resolved.size(),
                all.stream().filter(Incident::isAwaitingReporterInfo).count(),
                ratings.getCount() == 0 ? null : round(ratings.getAverage()),
                all.stream().filter(i -> i.getEscalationLevel() != com.itsm.enums.EscalationLevel.NONE
                        && !IncidentStates.closedStatuses().contains(i.getStatus())).count(),
                outageRepository.countByStatus(com.itsm.model.ServiceOutage.Status.ACTIVE),
                predictions.atRisk().size(),
                compliance,
                resolved.isEmpty() ? null
                        : round(resolved.stream().mapToLong(AnalyticsService::resolutionMinutes).average().orElse(0)),
                countBy(all, Incident::getStatus, IncidentStatus.class),
                countBy(all, Incident::getPriority, Priority.class),
                countBy(all, Incident::getCategory, IncidentCategory.class),
                byService(all),
                mttrByPriority,
                perDay,
                technicianStats(all));
    }

    private List<AnalyticsSummary.TechnicianStats> technicianStats(List<Incident> all) {
        return technicianRepository.findAll().stream().map(t -> {
            List<Incident> theirs = all.stream()
                    .filter(i -> i.getAssignedTechnician() != null && t.getId().equals(i.getAssignedTechnician().getId()))
                    .toList();
            long active = theirs.stream().filter(i -> IncidentStates.of(i.getStatus()).isActiveWork()).count();
            long done = theirs.stream().filter(i -> i.getResolvedAt() != null).count();
            return new AnalyticsSummary.TechnicianStats(t.getId(), t.getName(), t.isAvailable(), t.getSkills(),
                    active, done);
        }).toList();
    }

    /** Incidents per affected IT service, busiest first ("service analysis"). */
    private static Map<String, Long> byService(List<Incident> incidents) {
        Map<String, Long> counts = incidents.stream()
                .flatMap(i -> i.getAffectedServices().stream())
                .collect(Collectors.groupingBy(s -> s.getName(), Collectors.counting()));
        Map<String, Long> sorted = new LinkedHashMap<>();
        counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .forEach(e -> sorted.put(e.getKey(), e.getValue()));
        return sorted;
    }

    /** Counts per enum value, including zeros so charts always show every bar. */
    private static <E extends Enum<E>> Map<E, Long> countBy(List<Incident> incidents, Function<Incident, E> key,
                                                            Class<E> type) {
        Map<E, Long> counts = new EnumMap<>(type);
        for (E value : type.getEnumConstants()) {
            counts.put(value, 0L);
        }
        incidents.forEach(i -> counts.merge(key.apply(i), 1L, Long::sum));
        return counts;
    }

    private static long resolutionMinutes(Incident incident) {
        return Duration.between(incident.getCreatedAt(), incident.getResolvedAt()).toMinutes();
    }

    private static double round(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
