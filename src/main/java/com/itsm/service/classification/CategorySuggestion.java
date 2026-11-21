package com.itsm.service.classification;

import java.util.List;

import com.itsm.enums.IncidentCategory;

/** The classifier's best guess, how sure it is (0-1), and the runners-up. */
public record CategorySuggestion(IncidentCategory category, double confidence, List<Alternative> alternatives,
                                 String classifier) {

    public record Alternative(IncidentCategory category, double probability) {
    }
}
