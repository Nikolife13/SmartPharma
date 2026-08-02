package com.smartpharma.controller;

import com.smartpharma.dto.OrderSuggestion;
import com.smartpharma.service.PredictionClient;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Read-only ML reorder suggestions ("what should I order and how much"). Kept
// separate from OrderController, which owns the actual persisted order workflow -
// this one never touches the orders/order_items tables.
@RestController
@RequestMapping("/api/orders")
@PreAuthorize("hasRole('MANAGER')")
public class OrdersController {

    private final PredictionClient predictionClient;

    public OrdersController(PredictionClient predictionClient) {
        this.predictionClient = predictionClient;
    }

    @GetMapping("/suggestions")
    public List<OrderSuggestion> getSuggestions() {
        return predictionClient.getSuggestions();
    }
}
