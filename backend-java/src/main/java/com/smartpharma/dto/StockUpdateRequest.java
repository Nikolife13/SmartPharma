package com.smartpharma.dto;

import lombok.Data;

@Data
public class StockUpdateRequest {
    private Integer quantityChange;
    private String reason;   // SALE, EXPIRED, RESTOCK
}