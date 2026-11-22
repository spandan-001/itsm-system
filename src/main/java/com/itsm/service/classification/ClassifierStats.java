package com.itsm.service.classification;

import java.time.LocalDateTime;
import java.util.Map;

import com.itsm.enums.IncidentCategory;

/** What the classifier was trained on and how well it does, for the admin view and the report. */
public record ClassifierStats(
        String classifier,
        int seedExamples,
        int historicalExamples,
        int vocabularySize,
        Map<IncidentCategory, Long> examplesPerCategory,
        Double crossValidatedAccuracy,
        boolean autoCorrect,
        double autoCorrectThreshold,
        LocalDateTime trainedAt) {
}
