package com.itsm.service.classification;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.itsm.enums.IncidentCategory;

/** Reads labelled examples from a tab-separated file: {@code CATEGORY<TAB>text}, {@code #} for comments. */
public final class TrainingData {

    public static final String SEED_RESOURCE = "/classifier/training-data.tsv";

    private TrainingData() {
    }

    public static List<LabeledText> seed() {
        try (InputStream in = TrainingData.class.getResourceAsStream(SEED_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Missing classifier training data " + SEED_RESOURCE);
            }
            return parse(in);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    static List<LabeledText> parse(InputStream in) throws IOException {
        List<LabeledText> examples = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line;
        int number = 0;
        while ((line = reader.readLine()) != null) {
            number++;
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split("\t", 2);
            if (parts.length != 2) {
                throw new IllegalStateException("Training data line " + number + " needs CATEGORY<TAB>text");
            }
            examples.add(new LabeledText(IncidentCategory.valueOf(parts[0].trim()), parts[1].trim()));
        }
        return examples;
    }
}
