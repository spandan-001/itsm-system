package com.itsm.classification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.itsm.enums.IncidentCategory;
import com.itsm.service.classification.CategorySuggestion;
import com.itsm.service.classification.ClassifierEvaluation;
import com.itsm.service.classification.LabeledText;
import com.itsm.service.classification.NaiveBayesIncidentClassifier;
import com.itsm.service.classification.TrainingData;

class NaiveBayesClassifierTest {

    static List<LabeledText> seed;
    static NaiveBayesIncidentClassifier classifier;

    @BeforeAll
    static void train() {
        seed = TrainingData.seed();
        classifier = new NaiveBayesIncidentClassifier();
        classifier.train(seed);
    }

    @Test
    void seedDataCoversEveryCategory() {
        assertThat(seed).extracting(LabeledText::category).containsAll(List.of(IncidentCategory.values()));
        assertThat(seed).hasSizeGreaterThanOrEqualTo(150);
    }

    @Test
    void classifiesTheProblemStatementScenarioAsApplication() {
        CategorySuggestion suggestion = classifier.classify("Payment app down",
                "The organization's payment application is unavailable and users are unable to complete transactions.");

        assertThat(suggestion.category()).isEqualTo(IncidentCategory.APPLICATION);
        assertThat(suggestion.classifier()).isEqualTo("naive-bayes");
    }

    @Test
    void classifiesUnseenReportsWrittenInEverydayLanguage() {
        assertThat(classifier.classify("Can't get online in hostel", "wifi drops every few minutes").category())
                .isEqualTo(IncidentCategory.NETWORK);
        assertThat(classifier.classify("Reset link not arriving", "I forgot my password").category())
                .isEqualTo(IncidentCategory.AUTHENTICATION);
        assertThat(classifier.classify("Printer jammed again", "paper stuck in the tray").category())
                .isEqualTo(IncidentCategory.HARDWARE);
        assertThat(classifier.classify("Strange email", "asks me to confirm my bank login, looks like phishing").category())
                .isEqualTo(IncidentCategory.SECURITY);
    }

    @Test
    void probabilitiesAreRankedAndSensible() {
        CategorySuggestion suggestion = classifier.classify("Shared drive full", "cannot save files to the network share");

        assertThat(suggestion.confidence()).isBetween(0.0, 1.0);
        assertThat(suggestion.alternatives()).hasSize(3)
                .allSatisfy(a -> assertThat(a.probability()).isLessThanOrEqualTo(suggestion.confidence()));
        double total = suggestion.confidence()
                + suggestion.alternatives().stream().mapToDouble(CategorySuggestion.Alternative::probability).sum();
        assertThat(total).isLessThanOrEqualTo(1.0 + 1e-6);
    }

    @Test
    void unknownWordsGiveALowConfidenceGuess() {
        CategorySuggestion suggestion = classifier.classify("zxqv blorp", "wibble");

        assertThat(suggestion.confidence()).isLessThan(0.3); // close to the class priors (~1/9)
    }

    @Test
    void crossValidatedAccuracyIsWellAboveChance() {
        double accuracy = ClassifierEvaluation.crossValidatedAccuracy(NaiveBayesIncidentClassifier::new, seed, 5);

        // Chance is 1/9 (11%); the model must do much better on reports it has not seen.
        assertThat(accuracy).isGreaterThan(0.75);
    }

    @Test
    void mustBeTrainedBeforeUse() {
        assertThatThrownBy(() -> new NaiveBayesIncidentClassifier().classify("x", "y"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new NaiveBayesIncidentClassifier().train(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(classifier.vocabularySize()).isGreaterThan(300);
        assertThat(classifier.classify("router", "").confidence()).isCloseTo(classifier.classify("router", "").confidence(), within(0.0));
    }
}
