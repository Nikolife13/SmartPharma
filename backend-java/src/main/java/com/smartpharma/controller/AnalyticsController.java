package com.smartpharma.controller;

import com.smartpharma.dto.AnalyticsSummary;
import com.smartpharma.service.AnalyticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/summary")
    public AnalyticsSummary getSummary(@RequestParam(defaultValue = "monthly") String period) {
        AnalyticsService.Period parsed = "weekly".equalsIgnoreCase(period)
                ? AnalyticsService.Period.WEEKLY
                : AnalyticsService.Period.MONTHLY;
        return analyticsService.getSummary(parsed);
    }
}
