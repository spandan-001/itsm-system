package com.itsm.service.duplicate;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.itsm.service.text.TextTokenizer;

/** Cosine similarity of term-frequency vectors built by {@link TextTokenizer}. */
@Component
public class CosineTextSimilarity implements TextSimilarity {

    @Override
    public double similarity(String a, String b) {
        Map<String, Integer> va = termFrequencies(a);
        Map<String, Integer> vb = termFrequencies(b);
        if (va.isEmpty() || vb.isEmpty()) {
            return 0.0;
        }
        double dot = 0;
        for (Map.Entry<String, Integer> entry : va.entrySet()) {
            dot += entry.getValue() * vb.getOrDefault(entry.getKey(), 0);
        }
        return dot / (norm(va) * norm(vb));
    }

    Map<String, Integer> termFrequencies(String text) {
        return TextTokenizer.termFrequencies(text);
    }

    static String stem(String word) {
        return TextTokenizer.stem(word);
    }

    private static double norm(Map<String, Integer> vector) {
        double sum = 0;
        for (int count : vector.values()) {
            sum += (double) count * count;
        }
        return Math.sqrt(sum);
    }
}
