package com.itsm.service.duplicate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itsm.enums.IncidentCategory;
import com.itsm.model.ITService;
import com.itsm.model.Incident;
import com.itsm.repository.IncidentRepository;
import com.itsm.state.IncidentStates;

/**
 * Duplicate incident detection (intelligent feature). Compares a report with
 * open, non-duplicate incidents of the same category from the last few days:
 * 60% title similarity, 40% description similarity, plus a bonus when they
 * affect the same IT service.
 */
@Service
public class DuplicateDetector {

    static final double TITLE_WEIGHT = 0.6;
    static final double DESCRIPTION_WEIGHT = 0.4;
    static final double SHARED_SERVICE_BONUS = 0.15;
    /** Matches below this are not worth showing at all. */
    static final double SUGGESTION_FLOOR = 0.3;

    private final TextSimilarity similarity;
    private final IncidentRepository incidentRepository;
    private final Clock clock;
    private final double threshold;
    private final int windowDays;

    public DuplicateDetector(TextSimilarity similarity, IncidentRepository incidentRepository, Clock clock,
                             @Value("${itsm.duplicates.threshold:0.55}") double threshold,
                             @Value("${itsm.duplicates.window-days:7}") int windowDays) {
        this.similarity = similarity;
        this.incidentRepository = incidentRepository;
        this.clock = clock;
        this.threshold = threshold;
        this.windowDays = windowDays;
    }

    public double threshold() {
        return threshold;
    }

    /** The best match at or above the threshold, if any. */
    @Transactional(readOnly = true)
    public Optional<ScoredIncident> findDuplicate(Incident report) {
        Set<Long> serviceIds = serviceIds(report.getAffectedServices());
        return candidates(report.getCategory(), report.getId()).stream()
                .map(c -> new ScoredIncident(c, score(report.getTitle(), report.getDescription(), serviceIds, c)))
                .filter(s -> s.score() >= threshold)
                .max(Comparator.comparingDouble(ScoredIncident::score));
    }

    /** Similar open incidents for a draft report, best first (for a "did you mean" check). */
    @Transactional(readOnly = true)
    public List<DuplicateMatch> findSimilar(String title, String description, IncidentCategory category,
                                            Collection<Long> serviceIds, int limit) {
        Set<Long> ids = serviceIds == null ? Set.of() : Set.copyOf(serviceIds);
        return candidates(category, null).stream()
                .map(c -> new ScoredIncident(c, score(title, description, ids, c)))
                .filter(s -> s.score() >= SUGGESTION_FLOOR)
                .sorted(Comparator.comparingDouble(ScoredIncident::score).reversed())
                .limit(limit)
                .map(s -> DuplicateMatch.of(s.incident(), s.score(), threshold))
                .toList();
    }

    double score(String title, String description, Set<Long> serviceIds, Incident existing) {
        double text = TITLE_WEIGHT * similarity.similarity(title, existing.getTitle())
                + DESCRIPTION_WEIGHT * similarity.similarity(description, existing.getDescription());
        boolean sharesService = serviceIds(existing.getAffectedServices()).stream().anyMatch(serviceIds::contains);
        return Math.min(1.0, text + (sharesService ? SHARED_SERVICE_BONUS : 0));
    }

    private List<Incident> candidates(IncidentCategory category, Long excludeId) {
        LocalDateTime since = LocalDateTime.now(clock).minusDays(windowDays);
        return incidentRepository
                .findByCategoryAndStatusNotInAndDuplicateOfIsNullAndCreatedAtAfter(
                        category, IncidentStates.closedStatuses(), since)
                .stream()
                .filter(i -> excludeId == null || !excludeId.equals(i.getId()))
                .toList();
    }

    private static Set<Long> serviceIds(List<ITService> services) {
        return services.stream().map(ITService::getId).filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
    }

    /** An incident with its similarity score. */
    public record ScoredIncident(Incident incident, double score) {
    }
}
