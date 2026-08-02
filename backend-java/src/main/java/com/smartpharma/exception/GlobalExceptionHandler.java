package com.smartpharma.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

// Turns exceptions thrown anywhere in a controller/service into a proper JSON error
// response, instead of a raw stack trace. This is the one place that decides which
// HTTP status each business error maps to.
@RestControllerAdvice
public class GlobalExceptionHandler {

    // AccessDeniedException comes from a failed @PreAuthorize check. It's a
    // RuntimeException too, so this handler must be declared separately - Spring picks
    // the most specific @ExceptionHandler match, otherwise it'd fall into the generic
    // 400 case below instead of the 403 it should be.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Access is denied"));
    }

    // Every service in this app signals business errors as `new RuntimeException("some
    // message")` rather than custom exception classes - this switch is what maps each
    // known message back to the right HTTP status. Anything unrecognized falls back to 400.
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
