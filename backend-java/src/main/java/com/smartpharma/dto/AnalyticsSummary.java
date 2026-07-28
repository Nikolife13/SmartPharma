package com.smartpharma.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsSummary {
    private List<TrendPoint> salesTrend;
    private List<ProductTotal> topProducts;
    private List<ReasonCount> reasonBreakdown;
}
