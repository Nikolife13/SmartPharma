package com.smartpharma.dto;

import lombok.Data;

// Body of PATCH /api/admin/suppliers/{id}/status - a Manager approving or rejecting
// a pending supplier account. status is "ACTIVE" or "REJECTED".
@Data
public class UpdateSupplierStatusRequest {
    private String status;
}
