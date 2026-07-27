package com.smartpharma.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Access is denied"));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        String message = ex.getMessage();
        HttpStatus status = switch (message == null ? "" : message) {
            case "Bad credentials" -> HttpStatus.UNAUTHORIZED;
            case "Username already exists" -> HttpStatus.CONFLICT;
            case "Product not found", "User not found", "Order not found", "Supplier not found" -> HttpStatus.NOT_FOUND;
            case "Not authorized", "Supplier is not active", "Supplier account is not active" -> HttpStatus.FORBIDDEN;
            case "Order has already been responded to" -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(Map.of("message", message != null ? message : "Unexpected error"));
    }
}
