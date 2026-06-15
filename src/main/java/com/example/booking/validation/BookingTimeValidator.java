package com.example.booking.validation;

import com.example.booking.dto.CreateBookingRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.Duration;

/**
 * Validator for the @ValidBookingTime annotation.
 *
 * It enforces the cross-field rules that single-field annotations can't express:
 *   1. startTime must be before endTime, so every booking has a positive duration.
 *   2. The booking must last at least 15 minutes and at most 8 hours.
 *
 * Null start/end values are left to @NotNull on the fields; whether the times are
 * in the future is left to @Future. If either time is null we skip these checks to
 * avoid a NullPointerException and let the field-level annotations report the error.
 */
public class BookingTimeValidator
        implements ConstraintValidator<ValidBookingTime, CreateBookingRequest> {

    private static final Duration MIN_DURATION = Duration.ofMinutes(15);
    private static final Duration MAX_DURATION = Duration.ofHours(8);

    @Override
    public void initialize(ValidBookingTime annotation) {
        // No initialisation needed
    }

    @Override
    public boolean isValid(CreateBookingRequest request,
                           ConstraintValidatorContext context) {

        // Let @NotNull on the fields report missing values.
        if (request.getStartTime() == null || request.getEndTime() == null) {
            return true;
        }

        if (!request.getStartTime().isBefore(request.getEndTime())) {
            return reject(context, "startTime", "startTime must be before endTime");
        }

        Duration duration = Duration.between(request.getStartTime(), request.getEndTime());
        if (duration.compareTo(MIN_DURATION) < 0) {
            return reject(context, "endTime",
                    "booking must be at least " + MIN_DURATION.toMinutes() + " minutes long");
        }
        if (duration.compareTo(MAX_DURATION) > 0) {
            return reject(context, "endTime",
                    "booking must not exceed " + MAX_DURATION.toHours() + " hours");
        }

        return true;
    }

    /** Replace the default message with a field-specific one and fail validation. */
    private boolean reject(ConstraintValidatorContext context, String field, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(field)
                .addConstraintViolation();
        return false;
    }
}
