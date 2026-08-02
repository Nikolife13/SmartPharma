package com.smartpharma.model;

import jakarta.persistence.*;
import lombok.*;

// A single login account. The same table holds all three actor types (Pharmacist,
// Manager, Supplier) - role decides what they can do (see SecurityConfig/@PreAuthorize
// checks), and supplierStatus is the extra approval gate that only applies to suppliers.
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "supplier_status")
    private SupplierStatus supplierStatus;

    public enum Role {
        PHARMACIST,
        MANAGER,
        SUPPLIER
    }

    // Only meaningful when role == SUPPLIER; PENDING until a Manager approves the
    // account, and every supplier-facing endpoint checks this before allowing access.
    public enum SupplierStatus {
        PENDING,
        ACTIVE,
        REJECTED
    }
}