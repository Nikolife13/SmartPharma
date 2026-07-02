package com.smartpharma.repository;

import com.smartpharma.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // Low stock is relative to each product's own threshold, not a single global cutoff.
    @Query("SELECT p FROM Product p WHERE p.currentQuantity <= p.minThreshold")
    List<Product> findLowStockProducts();

    List<Product> findByExpiryDateBefore(LocalDate date);
}
