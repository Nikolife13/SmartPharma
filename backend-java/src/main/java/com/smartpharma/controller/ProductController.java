package com.smartpharma.controller;

import com.smartpharma.dto.ProductRequest;
import com.smartpharma.dto.StockUpdateRequest;
import com.smartpharma.model.Product;
import com.smartpharma.model.User;
import com.smartpharma.repository.UserRepository;
import com.smartpharma.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final UserRepository userRepository;

    public ProductController(ProductService productService, UserRepository userRepository) {
        this.productService = productService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<Product> getAllProducts() {
        return productService.getAllProducts();
    }

    @PostMapping
    public Product addProduct(@RequestBody ProductRequest request) {
        return productService.addProduct(request);
    }

    @PutMapping("/{id}/stock")
    public Product updateStock(@PathVariable Long id,
                           @RequestBody StockUpdateRequest request,
                           Authentication auth) {
             User user = userRepository.findByUsername(auth.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
            return productService.updateStock(id, request, user);
    }
}