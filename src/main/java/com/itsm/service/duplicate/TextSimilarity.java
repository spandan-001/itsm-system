package com.itsm.service.duplicate;

/** How alike two pieces of text are, from 0.0 (unrelated) to 1.0 (same). */
public interface TextSimilarity {

    double similarity(String a, String b);
}
