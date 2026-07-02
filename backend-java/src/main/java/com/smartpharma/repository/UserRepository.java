package com.smartpharma.repository;

import com.smartpharma.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * Spring Data JPA repository for User entity.
 */
public interface UserRepository extends JpaRepository<User, Long> {
    /**
     * Find a user by username (used during login).
     */
    Optional<User> findByUsername(String username);

    /**
     * Check if a username already exists (used during registration).
     */
    boolean existsByUsername(String username);
}