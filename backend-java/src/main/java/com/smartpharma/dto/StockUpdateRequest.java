package com.smartpharma.dto;

import lombok.Data;

// Body of PUT /api/products/{id}/stock - a manual stock in/out/expiry adjustment.
@Data
public class StockUpdateRequest {
    private Integer quantityChange;
    private String reason;   // SALE, EXPIRED, RESTOCK
}