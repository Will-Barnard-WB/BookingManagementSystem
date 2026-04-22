package com.example.booking.exception;

import com.example.booking.dto.ErrorResponse;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * Centralised exception handling for all controllers.
 *
 * Converts domain/validation exceptions into a consistent ErrorResponse JSON
 * envelope so clients always receive the same error schema regardless of the
 * exception type.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 404 — booking or related entity not found.
     */
    @ExceptionHandler(BookingNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(BookingNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage());
    }

    /**
     * 409 — the requested resource slot is already taken.
     */
    @ExceptionHandler(ResourceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleUnavailable(ResourceUnavailableException ex) {
        return buildResponse(HttpStatus.CONFLICT, "RESOURCE_UNAVAILABLE", ex.getMessage());
    }

    /**
     * 400 — a business rule was violated (e.g. invalid time range, duplicate email).
     */
    @ExceptionHandler(InvalidBookingException.class)
    public ResponseEntity<ErrorResponse> handleInvalidBooking(InvalidBookingException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "INVALID_BOOKING", ex.getMessage());
    }

    /**
     * 400 — a booking state transition is not permitted.
     */
    @ExceptionHandler(IllegalStateTransitionException.class)
    public ResponseEntity<ErrorResponse> handleIllegalTransition(IllegalStateTransitionException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "ILLEGAL_STATE_TRANSITION", ex.getMessage());
    }

    /**
     * 400 — Bean Validation (@Valid) failed on a request body.
     * Collects all field errors into a single human-readable message.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return buildResponse(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", message);
    }

    /**
     * 409 — optimistic lock failure (stale version on confirm/cancel) or a
     * SERIALIZABLE serialisation failure that escaped the proxy commit boundary.
     * The service catches these inside createBooking(), but failures at commit time
     * are surfaced as TransactionSystemException wrapping CannotAcquireLockException.
     */
    @ExceptionHandler({
        CannotAcquireLockException.class,
        ObjectOptimisticLockingFailureException.class
    })
    public ResponseEntity<ErrorResponse> handleConcurrencyFailure(Exception ex) {
        return buildResponse(HttpStatus.CONFLICT,
                "RESOURCE_UNAVAILABLE", "Booking conflict due to concurrent access. Please retry.");
    }

    @ExceptionHandler(TransactionSystemException.class)
    public ResponseEntity<ErrorResponse> handleTransactionSystem(TransactionSystemException ex) {
        Throwable cause = ex.getRootCause();
        if (cause instanceof java.sql.SQLException sqlEx
                && "40001".equals(sqlEx.getSQLState())) {
            // PostgreSQL serialization failure — treat as a booking conflict
            return buildResponse(HttpStatus.CONFLICT,
                    "RESOURCE_UNAVAILABLE", "Booking conflict due to concurrent access. Please retry.");
        }
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR", "An unexpected error occurred");
    }

    /**
     * 500 — catch-all for unexpected errors.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR", "An unexpected error occurred");
    }

    // -----------------------------------------------------------------------

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status,
                                                         String error,
                                                         String message) {
        ErrorResponse body = ErrorResponse.builder()
                .error(error)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
