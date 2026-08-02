package com.smartpharma.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
// One point on the Sales Trend chart - a time bucket (week/month/year label) and its total.
public class TrendPoint {
    private String label;
    private Integer totalUnits;
}
