package com.smartpharma.dto;

import lombok.Data;

// Body of POST /api/auth/register.
@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String email;
    /** Optional: PHARMACIST (default), MANAGER, or SUPPLIER. */
    private String role;
}