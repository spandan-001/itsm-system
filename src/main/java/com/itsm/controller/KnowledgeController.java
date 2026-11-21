package com.itsm.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.itsm.exception.OperationNotPermittedException;
import com.itsm.model.AccountLink.Role;
import com.itsm.service.AccountService;
import com.itsm.service.IncidentService;
import com.itsm.service.knowledge.ResolutionSuggestion;
import com.itsm.service.knowledge.ResolutionSuggestionService;

/** Fixes from similar past incidents, for the people resolving incidents. */
@RestController
public class KnowledgeController {

    private final ResolutionSuggestionService suggestions;
    private final IncidentService incidentService;
    private final AccountService accounts;

    public KnowledgeController(ResolutionSuggestionService suggestions, IncidentService incidentService,
                               AccountService accounts) {
        this.suggestions = suggestions;
        this.incidentService = incidentService;
        this.accounts = accounts;
    }

    @GetMapping("/api/incidents/{id}/suggested-resolutions")
    public List<ResolutionSuggestion> suggestedResolutions(@PathVariable Long id) {
        var account = accounts.requireCurrentAccount();
        if (account.role() == Role.EMPLOYEE) {
            throw new OperationNotPermittedException("Suggested resolutions are for technicians and administrators");
        }
        return suggestions.suggestFor(incidentService.getIncident(id, account));
    }
}
