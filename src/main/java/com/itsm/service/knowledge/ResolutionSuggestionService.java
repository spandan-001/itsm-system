package com.itsm.service.knowledge;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itsm.enums.IncidentStatus;
import com.itsm.model.ITService;
import com.itsm.model.Incident;
import com.itsm.repository.IncidentRepository;
import com.itsm.service.duplicate.TextSimilarity;

/**
 * Suggested resolution (intelligent feature): finds past incidents that look
 * like this one and offers the fixes recorded for them.
 *
 * <p>Score = 0.6 x title similarity + 0.4 x description similarity, plus
 * bonuses for the same category (+0.10) and the same IT service (+0.05), for a
 * fix the reporter confirmed by verifying or closing (+0.05), and for a well
 * rated fix (+0.03 per star above 3). The text part reuses the cosine
 * similarity from duplicate detection.
 */
@Service
public class ResolutionSuggestionService {

    static final double MIN_SCORE = 0.25;
    static final int LIMIT = 3;

    private final IncidentRepository incidents;
    private final TextSimilarity similarity;

    public ResolutionSuggestionService(IncidentRepository incidents, TextSimilarity similarity) {
        this.incidents = incidents;
        this.similarity = similarity;
    }

    @Transactional(readOnly = true)
    public List<ResolutionSuggestion> suggestFor(Incident incident) {
        Set<Long> services = serviceIds(incident);
        return incidents.findByResolutionSummaryIsNotNullAndDuplicateOfIsNull().stream()
                .filter(past -> !past.getId().equals(incident.getId()))
                .map(past -> toSuggestion(past, score(incident, services, past)))
                .filter(s -> s.score() >= MIN_SCORE)
                .sorted(Comparator.comparingDouble(ResolutionSuggestion::score).reversed())
                .limit(LIMIT)
                .toList();
    }

    double score(Incident incident, Set<Long> services, Incident past) {
        double text = 0.6 * similarity.similarity(incident.getTitle(), past.getTitle())
                + 0.4 * similarity.similarity(incident.getDescription(), past.getDescription());
        if (text == 0) {
            return 0; // bonuses alone never make an unrelated incident a match
        }
        double score = text;
        if (incident.getCategory() == past.getCategory()) {
            score += 0.10;
        }
        if (serviceIds(past).stream().anyMatch(services::contains)) {
            score += 0.05;
        }
        if (confirmed(past)) {
            score += 0.05;
        }
        if (past.getFeedbackRating() != null) {
            score += 0.03 * (past.getFeedbackRating() - 3);
        }
        return Math.max(0, score); // a ranking score, not a probability: bonuses may push it past 1
    }

    private static boolean confirmed(Incident incident) {
        return incident.getStatus() == IncidentStatus.VERIFIED || incident.getStatus() == IncidentStatus.CLOSED;
    }

    private static ResolutionSuggestion toSuggestion(Incident past, double score) {
        return new ResolutionSuggestion(past.getId(), past.getTitle(), past.getCategory(), past.getStatus(),
                past.getResolutionSummary(), past.getResolvedBy(), past.getFeedbackRating(), confirmed(past),
                Math.round(score * 1000) / 1000.0);
    }

    private static Set<Long> serviceIds(Incident incident) {
        return incident.getAffectedServices().stream().map(ITService::getId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}
