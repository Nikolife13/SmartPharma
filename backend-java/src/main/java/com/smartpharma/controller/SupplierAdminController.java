package com.smartpharma.controller;

import com.smartpharma.dto.SupplierSummary;
import com.smartpharma.dto.UpdateSupplierStatusRequest;
import com.smartpharma.service.SupplierService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/suppliers")
@PreAuthorize("hasRole('MANAGER')")
public class SupplierAdminController {

    private final SupplierService supplierService;

    public SupplierAdminController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    public List<SupplierSummary> listSuppliers(@RequestParam(required = false) String status) {
        return supplierService.listSuppliers(status);
    }

    @PatchMapping("/{id}/status")
    public SupplierSummary updateStatus(@PathVariable Long id, @RequestBody UpdateSupplierStatusRequest request) {
        return supplierService.updateStatus(id, request.getStatus());
    }
}
