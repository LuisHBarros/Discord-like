package com.luishbarros.discord_like.shared.adapters.middleware;

import com.luishbarros.discord_like.shared.domain.error.DomainError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class DomainErrorHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValid(MethodArgumentNotValidException error) {
        String message = error.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));

        if (message.isBlank()) {
            message = "Request validation failed";
        }

        Map<String, Object> body = Map.of(
                "error", "VALIDATION_ERROR",
                "message", message,
                "timestamp", Instant.now().toString()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(DomainError.class)
    public ResponseEntity<Map<String, Object>> handleDomainError(DomainError error) {
        HttpStatus status = mapErrorToStatus(error.getCode());

        Map<String, Object> body = Map.of(
                "error", error.getCode(),
                "message", error.getMessage(),
                "timestamp", Instant.now().toString()
        );

        return ResponseEntity.status(status).body(body);
    }

    private HttpStatus mapErrorToStatus(String code) {
        return switch (code) {
            case "INVALID_CREDENTIALS", "UNAUTHORIZED" -> HttpStatus.UNAUTHORIZED;
            case "FORBIDDEN", "USER_NOT_IN_ROOM" -> HttpStatus.FORBIDDEN;
            case "NOT_FOUND", "ROOM_NOT_FOUND", "USER_NOT_FOUND", "MESSAGE_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "DUPLICATE_EMAIL", "CONFLICT" -> HttpStatus.CONFLICT;
            case "RATE_LIMITED" -> HttpStatus.TOO_MANY_REQUESTS;
            case "VALIDATION_ERROR", "INVALID_MESSAGE", "INVALID_ROOM", "INVALID_USER",
                 "INVALID_INVITE_CODE", "ENCRYPTION_ERROR" -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }
}
