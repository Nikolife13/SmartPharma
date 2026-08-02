package com.smartpharma.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
// Returned by both login and register - the JWT plus enough user info for the
// frontend to route/gate the UI without a second round trip.
public class LoginResponse {
    private String token;
    private String username;
    private String role;
    private String supplierStatus;
}