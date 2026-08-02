package com.smartpharma.dto;

import lombok.Data;

// Body of POST /api/auth/login.
@Data
public class LoginRequest {
    private String username;
    private String password;
}