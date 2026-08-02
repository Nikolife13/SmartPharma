package com.smartpharma.service;

import com.smartpharma.dto.LoginRequest;
import com.smartpharma.dto.LoginResponse;
import com.smartpharma.dto.RegisterRequest;
import com.smartpharma.model.User;
import com.smartpharma.repository.UserRepository;
import com.smartpharma.security.JwtUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Handles user registration and login.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * Register a new user and return a JWT token.
     */
    public LoginResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        User.Role role = User.Role.PHARMACIST;
        if (request.getRole() != null && !request.getRole().isBlank()) {
            try {
                role = User.Role.valueOf(request.getRole().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid role: " + request.getRole());
            }
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setRole(role);
        // Suppliers self-register but can't act on anything until a Manager
        // approves them - see SupplierService.updateStatus / OrderService's checks.
        if (role == User.Role.SUPPLIER) {
            user.setSupplierStatus(User.SupplierStatus.PENDING);
        }
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getUsername());
        return toLoginResponse(user, token);
    }

    /**
     * Authenticate a user and return a JWT token.
     */
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Bad credentials"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Bad credentials");
        }
        String token = jwtUtil.generateToken(user.getUsername());
        return toLoginResponse(user, token);
    }

    private LoginResponse toLoginResponse(User user, String token) {
        String supplierStatus = user.getSupplierStatus() != null ? user.getSupplierStatus().name() : null;
        return new LoginResponse(token, user.getUsername(), user.getRole().name(), supplierStatus);
    }
}