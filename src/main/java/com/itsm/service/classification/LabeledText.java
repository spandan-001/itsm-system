package com.itsm.service.classification;

import com.itsm.enums.IncidentCategory;

/** One training example: report text and its correct category. */
public record LabeledText(IncidentCategory category, String text) {
}
