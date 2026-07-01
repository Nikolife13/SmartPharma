package com.smartpharma.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

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