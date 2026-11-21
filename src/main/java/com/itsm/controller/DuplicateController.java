package com.itsm.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.itsm.enums.IncidentCategory;
import com.itsm.model.Incident;
import com.itsm.service.IncidentService;
import com.itsm.service.duplicate.DuplicateDetector;
import com.itsm.service.duplicate.DuplicateMatch;

@RestController
public class DuplicateController {

    private final DuplicateDetector duplicateDetector;
    private final IncidentService incidentService;

    public DuplicateController(DuplicateDetector duplicateDetector, IncidentService incidentService) {
        this.duplicateDetector = duplicateDetector;
        this.incidentService = incidentService;
    }

    /** Check a draft report before submitting ("similar incidents already open"). */
    @GetMapping("/api/incidents/similar")
    public List<DuplicateMatch> similar(@RequestParam String title,
                                        @RequestParam(defaultValue = "") String description,
                                        @RequestParam IncidentCategory category,
                                        @RequestParam(required = false) List<Long> serviceIds,
                                        @RequestParam(defaultValue = "5") int limit) {
        return duplicateDetector.findSimilar(title, description, category, serviceIds, Math.min(limit, 20));
    }

    /** Incidents linked to this one as duplicates. */
    @GetMapping("/api/incidents/{id}/duplicates")
    public List<Incident> duplicates(@PathVariable Long id) {
        return incidentService.duplicatesOf(id);
    }

    /** Undo an automatic duplicate link: the incident gets its own SLA and is routed. */
    @PostMapping("/api/incidents/{id}/not-duplicate")
    public Incident notDuplicate(@PathVariable Long id) {
        return incidentService.markNotDuplicate(id);
    }
}
