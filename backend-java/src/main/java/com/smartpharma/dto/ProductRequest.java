package com.smartpharma.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ProductRequest {
    private String name;
    private String batchNumber;
    private LocalDate expiryDate;
    private Integer minThreshold;
    private Integer currentQuantity;
}