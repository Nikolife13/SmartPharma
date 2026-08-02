package com.smartpharma.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
// A product's sales history, packaged for the ML service's /predict endpoint.
// Built by PredictionClient from InventoryTransaction rows.
public class MlProductHistory {
    private Long productId;
    private String name;
    private Integer currentQuantity;
    private Integer minThreshold;
    private List<MlDailySale> dailySales;
}
