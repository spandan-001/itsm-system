package com.itsm.service.classification;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.itsm.enums.IncidentCategory;
import com.itsm.service.text.TextTokenizer;

/**
 * Multinomial Naive Bayes text classifier (intelligent feature: automatic
 * categorisation).
 *
 * <p>Training counts how often each term appears in each category. To classify,
 * it scores every category as
 * {@code log P(category) + sum over terms of log P(term | category)}, with
 * Laplace (+1) smoothing so unseen combinations are not impossible, and turns
 * the scores into probabilities with a softmax. Title words count twice, since
 * titles are short and on-topic. Words the model has never seen are ignored.
 *
 * <p>The trained model is an immutable snapshot swapped in atomically, so
 * classification can run while retraining happens.
 */
@Component
public class NaiveBayesIncidentClassifier implements IncidentClassifier {

    static final int TITLE_WEIGHT = 2;
    static final int ALTERNATIVES = 3;
    static final double MAX_CONFIDENCE = 0.999;

    private volatile Model model;

    @Override
    public String name() {
        return "naive-bayes";
    }

    @Override
    public String description() {
        return "Multinomial Naive Bayes over report terms, Laplace smoothing";
    }

    @Override
    public void train(List<LabeledText> examples) {
        if (examples == null || examples.isEmpty()) {
            throw new IllegalArgumentException("At least one training example is required");
        }
        Map<IncidentCategory, Map<String, Integer>> termCounts = new EnumMap<>(IncidentCategory.class);
        Map<IncidentCategory, Integer> totalTerms = new EnumMap<>(IncidentCategory.class);
        Map<IncidentCategory, Integer> documents = new EnumMap<>(IncidentCategory.class);
        Set<String> vocabulary = new HashSet<>();
        for (LabeledText example : examples) {
            documents.merge(example.category(), 1, Integer::sum);
            Map<String, Integer> counts = termCounts.computeIfAbsent(example.category(), c -> new HashMap<>());
            for (String term : features(example.text())) {
                counts.merge(term, 1, Integer::sum);
                totalTerms.merge(example.category(), 1, Integer::sum);
                vocabulary.add(term);
            }
        }
        this.model = new Model(termCounts, totalTerms, documents, examples.size(), Set.copyOf(vocabulary));
    }

    @Override
    public boolean isTrained() {
        return model != null;
    }

    @Override
    public int vocabularySize() {
        Model current = model;
        return current == null ? 0 : current.vocabulary().size();
    }

    @Override
    public CategorySuggestion classify(String title, String description) {
        Model current = model;
        if (current == null) {
            throw new IllegalStateException("The classifier has not been trained");
        }
        List<String> terms = new ArrayList<>();
        for (int i = 0; i < TITLE_WEIGHT; i++) {
            terms.addAll(features(title));
        }
        terms.addAll(features(description));
        terms.removeIf(term -> !current.vocabulary().contains(term));

        Map<IncidentCategory, Double> logScores = new EnumMap<>(IncidentCategory.class);
        int vocabularySize = current.vocabulary().size();
        for (var entry : current.documents().entrySet()) {
            IncidentCategory category = entry.getKey();
            double score = Math.log(entry.getValue() / (double) current.totalDocuments());
            Map<String, Integer> counts = current.termCounts().getOrDefault(category, Map.of());
            double denominator = current.totalTerms().getOrDefault(category, 0) + vocabularySize;
            for (String term : terms) {
                score += Math.log((counts.getOrDefault(term, 0) + 1) / denominator);
            }
            logScores.put(category, score);
        }
        Map<IncidentCategory, Double> probabilities = softmax(logScores);

        List<CategorySuggestion.Alternative> ranked = probabilities.entrySet().stream()
                .sorted(Map.Entry.<IncidentCategory, Double>comparingByValue(Comparator.reverseOrder()))
                .map(e -> new CategorySuggestion.Alternative(e.getKey(), round(e.getValue())))
                .toList();
        CategorySuggestion.Alternative best = ranked.get(0);
        return new CategorySuggestion(best.category(), best.probability(),
                ranked.subList(1, Math.min(ranked.size(), 1 + ALTERNATIVES)), name());
    }

    /** Normalised word tokens. (Word pairs were tried and lowered accuracy on this small data set.) */
    static List<String> features(String text) {
        return TextTokenizer.tokens(text);
    }

    /** exp(score) normalised to sum to 1, shifted by the maximum for numerical stability. */
    private static Map<IncidentCategory, Double> softmax(Map<IncidentCategory, Double> logScores) {
        double max = logScores.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);
        Map<IncidentCategory, Double> probabilities = new EnumMap<>(IncidentCategory.class);
        double sum = 0;
        for (var entry : logScores.entrySet()) {
            double p = Math.exp(entry.getValue() - max);
            probabilities.put(entry.getKey(), p);
            sum += p;
        }
        for (var entry : probabilities.entrySet()) {
            entry.setValue(entry.getValue() / sum);
        }
        return probabilities;
    }

    /** Three decimals, capped below 1: a probabilistic model is never certain. */
    private static double round(double value) {
        return Math.min(MAX_CONFIDENCE, Math.round(value * 1000) / 1000.0);
    }

    private record Model(Map<IncidentCategory, Map<String, Integer>> termCounts,
                         Map<IncidentCategory, Integer> totalTerms,
                         Map<IncidentCategory, Integer> documents,
                         int totalDocuments,
                         Set<String> vocabulary) {
    }
}
