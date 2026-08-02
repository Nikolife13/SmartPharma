package com.smartpharma.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
// A supplier account as shown to a Manager (pending-approval list, or the
// active-supplier picker on the Orders page). Never includes the password hash.
public class SupplierSummary {
    private Long id;
    private String username;
    private String email;
    private String supplierStatus;
}
