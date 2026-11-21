package com.itsm.service.classification;

import java.util.List;

import com.itsm.strategy.NamedStrategy;

/**
 * Suggests an incident category from free text. An interface so a different
 * model (keyword rules, an LLM) can replace Naive Bayes without other changes.
 */
public interface IncidentClassifier extends NamedStrategy {

    /** Replaces the model with one learned from these examples. */
    void train(List<LabeledText> examples);

    boolean isTrained();

    /** @throws IllegalStateException if not trained yet */
    CategorySuggestion classify(String title, String description);

    /** Distinct terms the model knows. */
    int vocabularySize();
}
