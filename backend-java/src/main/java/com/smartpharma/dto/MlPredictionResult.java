package com.smartpharma.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MlPredictionResult {
    private Long productId;
    private Integer forecastedDemand30d;
    private Integer suggestedOrderQty;
    private Integer confidenceScore;
}
