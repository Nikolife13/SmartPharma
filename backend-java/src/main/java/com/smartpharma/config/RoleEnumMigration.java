package com.smartpharma.config;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// The users.role column was created as a native MySQL ENUM back when only
// PHARMACIST and MANAGER existed. Hibernate's ddl-auto=update only adds missing
// tables/columns, it never widens an existing enum's allowed values, so
// inserting a SUPPLIER row failed in production with "Data truncated for
// column 'role'" even though the Java-side Role enum already had SUPPLIER.
// Safe to run on every boot: widening an enum to values it already allows is a
// no-op, and the try/catch keeps this from breaking startup on H2 (tests use
// ddl-auto=create-drop, so the column is already correct there).
@Component
@RequiredArgsConstructor
@Slf4j
public class RoleEnumMigration {

    private final EntityManager entityManager;

    @PostConstruct
    @Transactional
    public void widenRoleEnum() {
        try {
            entityManager.createNativeQuery(
                "ALTER TABLE users MODIFY COLUMN role ENUM('PHARMACIST','MANAGER','SUPPLIER') NOT NULL"
            ).executeUpdate();
        } catch (Exception e) {
            log.warn("Skipped widening users.role enum (expected on H2/test datasources): {}", e.getMessage());
        }
    }
}
