package com.itsm.service.duplicate;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

/**
 * Cosine similarity of term-frequency vectors, after lower-casing, dropping
 * stop words and stripping common suffixes ("printers" and "printer" match).
 */
@Component
public class CosineTextSimilarity implements TextSimilarity {

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the", "and", "or", "but", "is", "are", "was", "were", "be", "been", "am",
            "i", "my", "me", "we", "our", "you", "it", "its", "this", "that", "these", "those",
            "to", "of", "in", "on", "at", "for", "from", "with", "by", "as", "not", "no", "can",
            "cannot", "cant", "dont", "doesnt", "isnt", "wont", "do", "does", "did", "has", "have",
            "had", "please", "help", "issue", "problem", "any", "all", "since", "when", "there");

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
        Map<String, Integer> tf = new HashMap<>();
        if (text == null) {
            return tf;
        }
        for (String raw : text.toLowerCase(Locale.ROOT).replace("'", "").split("[^a-z0-9]+")) {
            if (raw.length() < 2 || STOP_WORDS.contains(raw)) {
                continue;
            }
            tf.merge(stem(raw), 1, Integer::sum);
        }
        return tf;
    }

    static String stem(String word) {
        for (String suffix : new String[] {"ing", "ed", "es", "s"}) {
            if (word.length() > suffix.length() + 3 && word.endsWith(suffix)) {
                return word.substring(0, word.length() - suffix.length());
            }
        }
        return word;
    }

    private static double norm(Map<String, Integer> vector) {
        double sum = 0;
        for (int count : vector.values()) {
            sum += (double) count * count;
        }
        return Math.sqrt(sum);
    }
}
