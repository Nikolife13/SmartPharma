package com.smartpharma.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupplierSummary {
    private Long id;
    private String username;
    private String email;
    private String supplierStatus;
}
