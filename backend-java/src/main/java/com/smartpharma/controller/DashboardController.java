package com.smartpharma.controller;

import com.smartpharma.model.Product;
import com.smartpharma.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public Map<String, List<Product>> getDashboard() {
        Map<String, List<Product>> result = new HashMap<>();
        result.put("lowStock", dashboardService.getLowStockProducts());
        result.put("nearExpiry", dashboardService.getNearExpiryProducts(LocalDate.now().plusDays(30)));
        return result;
    }
}