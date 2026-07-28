package com.smartpharma.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MlProductHistory {
    private Long productId;
    private String name;
    private Integer currentQuantity;
    private Integer minThreshold;
    private List<MlDailySale> dailySales;
}
