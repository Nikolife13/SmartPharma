package com.smartpharma.model;

import jakarta.persistence.*;
import lombok.*;

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

    public enum SupplierStatus {
        PENDING,
        ACTIVE,
        REJECTED
    }
}