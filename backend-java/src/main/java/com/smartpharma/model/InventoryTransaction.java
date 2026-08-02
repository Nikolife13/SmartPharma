package com.smartpharma.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

// The audit trail for stock movements: every time a product's quantity changes,
// one of these rows is written alongside it. This is also the raw data source
// for both the ML forecasts (sales history) and the Analytics charts.
@Entity
@Table(name = "inventory_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Reason reason;

    // Negative for stock leaving (SALE, EXPIRED), positive for stock coming in (RESTOCK).
    @Column(name = "quantity_change", nullable = false)
    private Integer quantityChange;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    public enum Reason {
        SALE,
        EXPIRED,
        RESTOCK
    }

    @PrePersist
    protected void onCreate() {
        transactionDate = LocalDateTime.now();
    }
}