package com.itsm.duplicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

import com.itsm.service.duplicate.CosineTextSimilarity;

class CosineTextSimilarityTest {

    private final CosineTextSimilarity similarity = new CosineTextSimilarity();

    @Test
    void identicalTextScoresOneAndUnrelatedScoresZero() {
        assertThat(similarity.similarity("Printer jammed on floor two", "printer JAMMED on floor two"))
                .isCloseTo(1.0, within(1e-9));
        assertThat(similarity.similarity("VPN certificate expired", "Printer out of toner")).isZero();
        assertThat(similarity.similarity("", "anything")).isZero();
    }

    @Test
    void ignoresStopWordsAndPluralsButRanksCloserTextHigher() {
        double close = similarity.similarity("The printers are jammed", "printer jammed");
        double far = similarity.similarity("The printers are jammed", "printer driver update needed");

        assertThat(close).isCloseTo(1.0, within(1e-9));
        assertThat(far).isLessThan(close).isGreaterThan(0.0);
    }
}
