package com.smartpharma.service;

import com.smartpharma.model.Product;
import com.smartpharma.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class DashboardService {

    private final ProductRepository productRepository;

    public DashboardService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> getLowStockProducts() {
        return productRepository.findLowStockProducts();
    }

    public List<Product> getNearExpiryProducts(LocalDate date) {
        return productRepository.findByExpiryDateBefore(date);
    }
}