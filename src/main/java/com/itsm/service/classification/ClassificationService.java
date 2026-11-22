package com.itsm.service.classification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.itsm.enums.IncidentCategory;
import com.itsm.enums.IncidentStatus;
import com.itsm.event.IncidentStatusChangedEvent;
import com.itsm.model.Incident;
import com.itsm.repository.IncidentRepository;

/**
 * Runs the category classifier for new incidents and keeps it trained.
 *
 * <p>The model learns from the seed examples plus every resolved incident, and
 * retrains whenever an incident is closed (Observer), so it improves with use.
 * When auto-correct is on and the model is confident enough that the reporter
 * picked the wrong category, the category is corrected before routing.
 */
@Service
public class ClassificationService {

    private static final Logger log = LoggerFactory.getLogger(ClassificationService.class);
    static final int FOLDS = 5;

    private final IncidentClassifier classifier;
    private final IncidentRepository incidentRepository;
    private volatile boolean autoCorrect;
    private volatile double threshold;
    private volatile ClassifierStats stats;

    public ClassificationService(IncidentClassifier classifier, IncidentRepository incidentRepository,
                                 @Value("${itsm.classifier.auto-correct:true}") boolean autoCorrect,
                                 @Value("${itsm.classifier.auto-correct-threshold:0.85}") double threshold) {
        this.classifier = classifier;
        this.incidentRepository = incidentRepository;
        this.autoCorrect = autoCorrect;
        this.threshold = validThreshold(threshold);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void trainOnStartup() {
        retrain();
    }

    /** Learn again from history once an incident is closed and its category is final. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onStatusChanged(IncidentStatusChangedEvent event) {
        if (event.getToStatus() == IncidentStatus.CLOSED) {
            retrain();
        }
    }

    /** Rebuilds the model from seed data and resolved incidents, and re-measures accuracy. */
    public synchronized ClassifierStats retrain() {
        List<LabeledText> seed = TrainingData.seed();
        List<LabeledText> history = incidentRepository.findAll().stream()
                .filter(i -> i.getResolvedAt() != null && !i.isDuplicate())
                .map(i -> new LabeledText(i.getCategory(), i.getTitle() + " " + i.getDescription()))
                .toList();
        List<LabeledText> all = new ArrayList<>(seed);
        all.addAll(history);
        classifier.train(all);

        double accuracy = ClassifierEvaluation.crossValidatedAccuracy(NaiveBayesIncidentClassifier::new, all, FOLDS);
        Map<IncidentCategory, Long> perCategory = new EnumMap<>(IncidentCategory.class);
        perCategory.putAll(all.stream().collect(Collectors.groupingBy(LabeledText::category, Collectors.counting())));
        stats = new ClassifierStats(classifier.name(), seed.size(), history.size(), classifier.vocabularySize(),
                perCategory, Double.isNaN(accuracy) ? null : Math.round(accuracy * 1000) / 1000.0,
                autoCorrect, threshold, LocalDateTime.now());
        log.info("Trained {} classifier on {} examples ({} from history), cross-validated accuracy {}",
                classifier.name(), all.size(), history.size(), stats.crossValidatedAccuracy());
        return stats;
    }

    public CategorySuggestion suggest(String title, String description) {
        if (!classifier.isTrained()) {
            retrain();
        }
        return classifier.classify(title, description);
    }

    /**
     * Records the suggestion on a new incident and, if auto-correct applies,
     * changes its category. Returns true if the category was changed.
     */
    public boolean classify(Incident incident) {
        CategorySuggestion suggestion = suggest(incident.getTitle(), incident.getDescription());
        return incident.applyClassification(suggestion.category(), suggestion.confidence(),
                autoCorrect ? threshold : null);
    }

    public ClassifierStats stats() {
        ClassifierStats current = stats == null ? retrain() : stats;
        return new ClassifierStats(current.classifier(), current.seedExamples(), current.historicalExamples(),
                current.vocabularySize(), current.examplesPerCategory(), current.crossValidatedAccuracy(),
                autoCorrect, threshold, current.trainedAt());
    }

    public void configure(boolean autoCorrect, double threshold) {
        this.threshold = validThreshold(threshold);
        this.autoCorrect = autoCorrect;
    }

    private static double validThreshold(double threshold) {
        if (threshold <= 0 || threshold > 1) {
            throw new IllegalArgumentException("Auto-correct threshold must be above 0 and at most 1");
        }
        return threshold;
    }
}
