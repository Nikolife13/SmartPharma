package com.smartpharma.model;

import jakarta.persistence.*;
import lombok.*;

// One product line within an Order. productName is a snapshot taken when the order
// was created, so the order still reads correctly even if the product is later renamed.
@Entity
@Table(name = "order_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "requested_qty", nullable = false)
    private Integer requestedQty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "confirmed_qty")
    private Integer confirmedQty;

    // Set by the supplier when responding to the order; PENDING until then.
    public enum Status {
        PENDING,
        AVAILABLE,
        UNAVAILABLE
    }

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = Status.PENDING;
        }
    }
}
