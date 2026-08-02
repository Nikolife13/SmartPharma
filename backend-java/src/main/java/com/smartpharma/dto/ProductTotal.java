package com.smartpharma.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
// One bar on the Top Products chart - a product name and its total units sold.
public class ProductTotal {
    private String productName;
    private Integer totalUnits;
}
