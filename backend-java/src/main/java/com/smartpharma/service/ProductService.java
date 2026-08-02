package com.smartpharma.service;

import com.smartpharma.dto.ProductRequest;
import com.smartpharma.dto.StockUpdateRequest;
import com.smartpharma.model.InventoryTransaction;
import com.smartpharma.model.Product;
import com.smartpharma.model.User;
import com.smartpharma.repository.InventoryTransactionRepository;
import com.smartpharma.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

// Owns the two things that change together: a product's live stock count, and
// the audit trail of why it changed (InventoryTransaction rows).
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final InventoryTransactionRepository transactionRepository;

    public ProductService(ProductRepository productRepository,
                          InventoryTransactionRepository transactionRepository) {
        this.productRepository = productRepository;
        this.transactionRepository = transactionRepository;
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product addProduct(ProductRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setBatchNumber(request.getBatchNumber());
        product.setExpiryDate(request.getExpiryDate());
        product.setMinThreshold(request.getMinThreshold());
        product.setCurrentQuantity(request.getCurrentQuantity());
        return productRepository.save(product);
    }

    // Applies a manual stock change (sale, restock, or expiry write-off) and records
    // it as a transaction in the same call, so current_quantity and the audit trail
    // never drift apart.
    public Product updateStock(Long productId, StockUpdateRequest request, User user) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        int change = request.getQuantityChange();
        String reasonStr = request.getReason();

        InventoryTransaction.Reason reason = InventoryTransaction.Reason.valueOf(reasonStr.toUpperCase());
        int newQuantity = product.getCurrentQuantity() + change;
        product.setCurrentQuantity(newQuantity);

        // Record the transaction so this change shows up in Analytics/ML history.
        InventoryTransaction txn = new InventoryTransaction();
        txn.setProduct(product);
        txn.setUser(user);
        txn.setReason(reason);
        txn.setQuantityChange(change);

        transactionRepository.save(txn);
        return productRepository.save(product);
    }
}