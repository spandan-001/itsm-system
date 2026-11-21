package com.itsm.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

import com.itsm.model.Incident;
import com.itsm.service.AccountService;
import com.itsm.service.search.IncidentSearchCriteria;
import com.itsm.service.search.IncidentSearchService;

@RestController
public class IncidentSearchController {

    private final IncidentSearchService searchService;

    private final AccountService accounts;

    public IncidentSearchController(IncidentSearchService searchService, AccountService accounts) {
        this.searchService = searchService;
        this.accounts = accounts;
    }

    @GetMapping("/api/admin/incidents")
    public PagedModel<Incident> adminSearch(
            @ModelAttribute IncidentSearchCriteria criteria,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return new PagedModel<>(searchService.search(criteria, pageable));
    }

    /**
     * Example: {@code /api/incidents/search?status=ASSIGNED&status=IN_PROGRESS&priority=P1
     * &category=NETWORK&keyword=wifi&createdFrom=2026-11-01&slaBreached=true&sort=priority}
     */
    @GetMapping("/api/incidents/search")
    public PagedModel<Incident> search(
            @ModelAttribute IncidentSearchCriteria criteria,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return new PagedModel<>(searchService.search(criteria, pageable, accounts.requireCurrentAccount()));
    }
}
