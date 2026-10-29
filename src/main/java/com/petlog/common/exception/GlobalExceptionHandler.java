package com.petlog.common.exception;

import com.petlog.auth.exception.EmailAlreadyExistsException;
import com.petlog.events.exception.ActiveWalkAlreadyExistsException;
import com.petlog.events.exception.EventNotFoundException;
import com.petlog.events.exception.EventValidationException;
import org.hibernate.exception.ConstraintViolationException;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(
        @NonNull DataIntegrityViolationException ex
    ) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation
                && "uq_events_active_walk_per_pet".equals(violation.getConstraintName())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(
                    HttpStatus.CONFLICT.value(),
                    "Pet already has active walk"
                ));
            }
        }
        throw ex;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> accessDeniedException(AccessDeniedException ex) {
        HttpStatus status = HttpStatus.FORBIDDEN;
        return ResponseEntity.status(status).body(new ApiError(status.value(), ex.getMessage()));
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleEmailAlreadyExists(
        EmailAlreadyExistsException ex
    ) {
        HttpStatus status = HttpStatus.CONFLICT;

        ApiError apiError = new ApiError(
            status.value(),
            ex.getMessage()
        );

        return ResponseEntity
            .status(status)
            .body(apiError);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> methodArgumentNotValidException(
        MethodArgumentNotValidException ex
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        String message = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .findFirst()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .orElse("Validation failed");

        ApiError apiError = new ApiError(
            status.value(),
            message
        );

        return ResponseEntity
            .status(status)
            .body(apiError);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> badCredentialsException() {
        HttpStatus status = HttpStatus.UNAUTHORIZED;

        ApiError apiError = new ApiError(
            status.value(),
            "Invalid email or password"
        );

        return ResponseEntity
            .status(status)
            .body(apiError);
    }

    @ExceptionHandler(EventNotFoundException.class)
    public ResponseEntity<ApiError> eventNotFoundException(
        EventNotFoundException ex
    ) {
        HttpStatus status = HttpStatus.NOT_FOUND;

        ApiError apiError = new ApiError(
            status.value(),
            ex.getMessage()
        );

        return ResponseEntity
            .status(status)
            .body(apiError);
    }

    @ExceptionHandler(EventValidationException.class)
    public ResponseEntity<ApiError> eventValidationException(
        EventValidationException ex
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        ApiError apiError = new ApiError(
            status.value(),
            ex.getMessage()
        );

        return ResponseEntity
            .status(status)
            .body(apiError);
    }

    @ExceptionHandler(ActiveWalkAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleActiveWalkAlreadyExists(
        ActiveWalkAlreadyExistsException ex
    ) {
        HttpStatus status = HttpStatus.CONFLICT;

        ApiError apiError = new ApiError(
            status.value(),
            ex.getMessage()
        );

        return ResponseEntity
            .status(status)
            .body(apiError);
    }
}
