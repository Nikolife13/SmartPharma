package com.smartpharma.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

// The users.role column was created as a native MySQL ENUM back when only
// PHARMACIST and MANAGER existed. Hibernate's ddl-auto=update only adds missing
// tables/columns, it never widens an existing enum's allowed values, so
// inserting a SUPPLIER row failed in production with "Data truncated for
// column 'role'" even though the Java-side Role enum already had SUPPLIER.
// Uses a plain JDBC connection rather than EntityManager: @Transactional does
// not apply to @PostConstruct methods (the AOP proxy isn't wired yet when the
// lifecycle callback runs), and DDL statements like ALTER TABLE auto-commit in
// MySQL anyway, so no Spring-managed transaction is needed here.
// Safe to run on every boot: widening an enum to values it already allows is a
// no-op, and the try/catch keeps this from breaking startup on H2 (tests use
// ddl-auto=create-drop, so the column is already correct there).
@Component
@RequiredArgsConstructor
@Slf4j
public class RoleEnumMigration {

    private final DataSource dataSource;

    @PostConstruct
    public void widenRoleEnum() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE users MODIFY COLUMN role ENUM('PHARMACIST','MANAGER','SUPPLIER') NOT NULL");
            log.info("users.role enum widened to include SUPPLIER");
        } catch (Exception e) {
            log.warn("Skipped widening users.role enum (expected on H2/test datasources): {}", e.getMessage());
        }
    }
}
