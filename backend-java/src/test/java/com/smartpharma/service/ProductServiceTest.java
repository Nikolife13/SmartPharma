package com.smartpharma.service;

import com.smartpharma.dto.ProductRequest;
import com.smartpharma.dto.StockUpdateRequest;
import com.smartpharma.model.InventoryTransaction;
import com.smartpharma.model.Product;
import com.smartpharma.model.User;
import com.smartpharma.repository.InventoryTransactionRepository;
import com.smartpharma.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for stock in/out/expiry handling - the one place current_quantity
 * and the inventory_transactions audit trail are updated together.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private InventoryTransactionRepository transactionRepository;

    @InjectMocks
    private ProductService productService;

    private Product product;
    private User user;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(1L);
        product.setName("Amoxicillin");
        product.setCurrentQuantity(20);

        user = new User();
        user.setId(5L);
        user.setUsername("pharmacist1");
        user.setRole(User.Role.PHARMACIST);
    }

    @Test
    void updateStock_restock_increasesQuantity() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        StockUpdateRequest request = new StockUpdateRequest();
        request.setQuantityChange(15);
        request.setReason("RESTOCK");

        Product result = productService.updateStock(1L, request, user);

        assertThat(result.getCurrentQuantity()).isEqualTo(35);
    }

    @Test
    void updateStock_sale_decreasesQuantity() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        StockUpdateRequest request = new StockUpdateRequest();
        request.setQuantityChange(-8);
        request.setReason("SALE");

        Product result = productService.updateStock(1L, request, user);

        assertThat(result.getCurrentQuantity()).isEqualTo(12);
    }

    @Test
    void updateStock_recordsAnInventoryTransactionMatchingTheChange() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        StockUpdateRequest request = new StockUpdateRequest();
        request.setQuantityChange(-5);
        request.setReason("EXPIRED");

        productService.updateStock(1L, request, user);

        ArgumentCaptor<InventoryTransaction> captor = ArgumentCaptor.forClass(InventoryTransaction.class);
        verify(transactionRepository).save(captor.capture());

        InventoryTransaction saved = captor.getValue();
        assertThat(saved.getProduct()).isEqualTo(product);
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getReason()).isEqualTo(InventoryTransaction.Reason.EXPIRED);
        assertThat(saved.getQuantityChange()).isEqualTo(-5);
    }

    @Test
    void updateStock_unknownProduct_throws() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        StockUpdateRequest request = new StockUpdateRequest();
        request.setQuantityChange(1);
        request.setReason("SALE");

        assertThatThrownBy(() -> productService.updateStock(99L, request, user))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Product not found");
    }

    @Test
    void addProduct_mapsEveryFieldFromTheRequest() {
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ProductRequest request = new ProductRequest();
        request.setName("Ibuprofen");
        request.setBatchNumber("B999");
        request.setExpiryDate(LocalDate.of(2027, 6, 1));
        request.setMinThreshold(10);
        request.setCurrentQuantity(50);

        Product result = productService.addProduct(request);

        assertThat(result.getName()).isEqualTo("Ibuprofen");
        assertThat(result.getBatchNumber()).isEqualTo("B999");
        assertThat(result.getExpiryDate()).isEqualTo(LocalDate.of(2027, 6, 1));
        assertThat(result.getMinThreshold()).isEqualTo(10);
        assertThat(result.getCurrentQuantity()).isEqualTo(50);
    }
}
