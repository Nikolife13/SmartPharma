package com.smartpharma.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
// One slice of the Transaction Breakdown donut chart (SALE/RESTOCK/EXPIRED and its count).
public class ReasonCount {
    private String reason;
    private Long count;
}
