package com.iitsm.exception;

import java.util.*;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@org.springframework.core.annotation.Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(AppException.class)
  ResponseEntity<?> app(AppException e) {
    return error(e.getStatus(), e.getMessage());
  }

  @ExceptionHandler({
    IllegalArgumentException.class,
    java.time.DateTimeException.class,
    org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
    org.springframework.http.converter.HttpMessageNotReadableException.class
  })
  ResponseEntity<?> bad(Exception e) {
    return error(
        HttpStatus.BAD_REQUEST,
        "Invalid request: "
            + (e instanceof IllegalArgumentException
                ? e.getMessage()
                : "Check field types and JSON structure"));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<?> validation(MethodArgumentNotValidException e) {
    return error(
        HttpStatus.BAD_REQUEST,
        e.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + ": " + f.getDefaultMessage())
            .reduce((a, b) -> a + "; " + b)
            .orElse("Invalid request"));
  }

  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<?> denied() {
    return error(HttpStatus.FORBIDDEN, "You do not have permission for this operation");
  }

  @ExceptionHandler({
    DataIntegrityViolationException.class,
    OptimisticLockingFailureException.class
  })
  ResponseEntity<?> conflict() {
    return error(
        HttpStatus.CONFLICT,
        "A conflicting record or concurrent update exists. Refresh and retry.");
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<?> internal(Exception e) {
    org.slf4j.LoggerFactory.getLogger(getClass()).error("Request failed", e);
    return error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
  }

  private ResponseEntity<?> error(HttpStatus s, String m) {
    return ResponseEntity.status(s)
        .body(
            Map.of(
                "status",
                s.value(),
                "message",
                m,
                "timestamp",
                java.time.Instant.now().toString()));
  }
}
