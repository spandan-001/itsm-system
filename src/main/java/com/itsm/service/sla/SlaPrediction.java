package com.itsm.service.sla;

import java.time.LocalDateTime;

import com.itsm.enums.SlaRisk;

/**
 * When an open incident is likely to be resolved, and whether that beats its deadline.
 *
 * @param basis           what the estimate was based on, in words
 * @param sampleSize      how many past incidents the estimate used (0 for the fallback)
 * @param workloadFactor  multiplier applied for the technician's other open work
 */
public record SlaPrediction(
        Long incidentId,
        String title,
        LocalDateTime predictedResolutionAt,
        LocalDateTime resolutionDueAt,
        SlaRisk risk,
        Long marginMinutes,
        String basis,
        int sampleSize,
        double workloadFactor) {
}
