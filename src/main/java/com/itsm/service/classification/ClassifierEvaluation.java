package com.itsm.service.classification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

/** Measures a classifier with k-fold cross-validation: train on k-1 parts, test on the held-out part. */
public final class ClassifierEvaluation {

    private ClassifierEvaluation() {
    }

    /** Fraction of held-out examples classified correctly, from 0 to 1. Deterministic (fixed shuffle). */
    public static double crossValidatedAccuracy(Supplier<IncidentClassifier> factory, List<LabeledText> examples,
                                                int folds) {
        if (examples.size() < folds) {
            return Double.NaN;
        }
        List<LabeledText> shuffled = new ArrayList<>(examples);
        Collections.shuffle(shuffled, new Random(42));
        int correct = 0;
        for (int fold = 0; fold < folds; fold++) {
            List<LabeledText> training = new ArrayList<>();
            List<LabeledText> held = new ArrayList<>();
            for (int i = 0; i < shuffled.size(); i++) {
                (i % folds == fold ? held : training).add(shuffled.get(i));
            }
            IncidentClassifier classifier = factory.get();
            classifier.train(training);
            for (LabeledText example : held) {
                if (classifier.classify(example.text(), "").category() == example.category()) {
                    correct++;
                }
            }
        }
        return correct / (double) shuffled.size();
    }
}
