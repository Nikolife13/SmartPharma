package com.smartpharma.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
// A single "you should reorder this" row shown on the Orders page, combining the
// product's current stock with the ML service's forecast for it.
public class OrderSuggestion {
    private Long productId;
    private String productName;
    private String batchNumber;
    private Integer currentQuantity;
    private Integer minThreshold;
    private Integer forecastedDemand;
    private Integer suggestedOrderQty;
    private Integer confidenceScore;
}
