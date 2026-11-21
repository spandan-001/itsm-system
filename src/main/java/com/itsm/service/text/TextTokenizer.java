package com.itsm.service.text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Turns free text into normalised terms: lower case, punctuation split, stop
 * words dropped, common suffixes stripped ("printers" and "printer" match).
 * Shared by duplicate detection and the incident classifier.
 */
public final class TextTokenizer {

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the", "and", "or", "but", "is", "are", "was", "were", "be", "been", "am",
            "i", "my", "me", "we", "our", "you", "it", "its", "this", "that", "these", "those",
            "to", "of", "in", "on", "at", "for", "from", "with", "by", "as", "not", "no", "can",
            "cannot", "cant", "dont", "doesnt", "isnt", "wont", "do", "does", "did", "has", "have",
            "had", "please", "help", "issue", "problem", "any", "all", "since", "when", "there",
            "so", "very", "today", "now", "again", "still", "after", "before", "keep", "keeps", "get", "gets");

    private TextTokenizer() {
    }

    public static List<String> tokens(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null) {
            return tokens;
        }
        for (String raw : text.toLowerCase(Locale.ROOT).replace("'", "").split("[^a-z0-9]+")) {
            if (raw.length() >= 2 && !STOP_WORDS.contains(raw)) {
                tokens.add(stem(raw));
            }
        }
        return tokens;
    }

    public static Map<String, Integer> termFrequencies(String text) {
        Map<String, Integer> tf = new HashMap<>();
        for (String token : tokens(text)) {
            tf.merge(token, 1, Integer::sum);
        }
        return tf;
    }

    public static String stem(String word) {
        for (String suffix : new String[] {"ing", "ed", "es", "s"}) {
            if (word.length() > suffix.length() + 3 && word.endsWith(suffix)) {
                return word.substring(0, word.length() - suffix.length());
            }
        }
        return word;
    }
}
