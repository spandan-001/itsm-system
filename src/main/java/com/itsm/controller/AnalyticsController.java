package com.itsm.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.itsm.service.analytics.AnalyticsService;
import com.itsm.service.analytics.AnalyticsSummary;

@RestController
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/api/analytics/summary")
    public AnalyticsSummary summary() {
        return analyticsService.summary();
    }
}
