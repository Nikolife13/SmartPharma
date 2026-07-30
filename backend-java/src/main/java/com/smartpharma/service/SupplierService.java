package com.smartpharma.service;

import com.smartpharma.dto.SupplierSummary;
import com.smartpharma.model.User;
import com.smartpharma.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SupplierService {

    private final UserRepository userRepository;

    public SupplierService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<SupplierSummary> listSuppliers(String statusFilter) {
        List<User> suppliers = userRepository.findByRole(User.Role.SUPPLIER);
        return suppliers.stream()
                .filter(s -> statusFilter == null || statusFilter.isBlank()
                        || s.getSupplierStatus() == User.SupplierStatus.valueOf(statusFilter.toUpperCase()))
                .map(this::toSummary)
                .toList();
    }

    public SupplierSummary updateStatus(Long supplierId, String newStatus) {
        User supplier = userRepository.findById(supplierId)
                .filter(u -> u.getRole() == User.Role.SUPPLIER)
                .orElseThrow(() -> new RuntimeException("Supplier not found"));

        User.SupplierStatus status;
        try {
            status = User.SupplierStatus.valueOf(newStatus.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid supplier status: " + newStatus);
        }

        supplier.setSupplierStatus(status);
        userRepository.save(supplier);
        return toSummary(supplier);
    }

    private SupplierSummary toSummary(User supplier) {
        return new SupplierSummary(
                supplier.getId(),
                supplier.getUsername(),
                supplier.getEmail(),
                supplier.getSupplierStatus() != null ? supplier.getSupplierStatus().name() : null
        );
    }
}
