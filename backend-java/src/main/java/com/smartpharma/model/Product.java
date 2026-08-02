package com.smartpharma.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

// A single medicine/batch tracked in the pharmacy's stock. currentQuantity is the
// live count on the shelf; it only ever changes through ProductService.updateStock(),
// which also writes a matching InventoryTransaction row so every change is auditable.
@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "batch_number")
    private String batchNumber;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "min_threshold")
    private Integer minThreshold;

    @Column(name = "current_quantity")
    private Integer currentQuantity;

    @Column(name = "created_at")
    private LocalDate createdAt;

    @Column(name = "updated_at")
    private LocalDate updatedAt;

    // Auto-stamp created/updated dates so callers never have to set them manually.
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDate.now();
        updatedAt = LocalDate.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDate.now();
    }
}