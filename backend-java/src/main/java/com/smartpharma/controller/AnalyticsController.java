package com.smartpharma.controller;

import com.smartpharma.dto.AnalyticsSummary;
import com.smartpharma.service.AnalyticsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Backs the three Analytics charts. Manager-only - a Pharmacist can operate stock
// but doesn't need business-level sales analytics.
@RestController
@RequestMapping("/api/analytics")
@PreAuthorize("hasRole('MANAGER')")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/summary")
    public AnalyticsSummary getSummary(@RequestParam(defaultValue = "monthly") String period) {
        AnalyticsService.Period parsed;
        if ("weekly".equalsIgnoreCase(period)) {
            parsed = AnalyticsService.Period.WEEKLY;
        } else if ("yearly".equalsIgnoreCase(period)) {
            parsed = AnalyticsService.Period.YEARLY;
        } else {
            parsed = AnalyticsService.Period.MONTHLY;
        }
        return analyticsService.getSummary(parsed);
    }
}
