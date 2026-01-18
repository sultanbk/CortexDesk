package com.prodapt.network_ticketing.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import io.swagger.v3.oas.annotations.Hidden;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
@Hidden
public class GlobalExceptionHandler {

    // 🔐 Authentication / login errors
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntime(RuntimeException ex) {

        if ("Invalid credentials".equals(ex.getMessage())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(error(HttpStatus.UNAUTHORIZED, ex.getMessage()));
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(error(HttpStatus.BAD_REQUEST, ex.getMessage()));
    }

    // 🚫 Authorization errors (role mismatch)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(error(HttpStatus.FORBIDDEN, "Access denied"));
    }

    // 💥 Any unexpected error
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong"));
    }

    private Map<String, Object> error(HttpStatus status, String message) {
        return Map.of(
                "timestamp", LocalDateTime.now(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message
        );
    }
}
