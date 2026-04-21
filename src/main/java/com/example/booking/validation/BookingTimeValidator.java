package com.example.booking.validation;

import com.example.booking.dto.CreateBookingRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for the @ValidBookingTime annotation.
 *
 * Rules:
 *   1. startTime must not be null AND endTime must not be null
 *      (null checks are delegated to @NotNull on the fields — if either is
 *       null here we skip cross-field validation to avoid a NullPointerException)
 *
 *   2. startTime must be strictly before endTime
 *      Ensures a booking always has a positive duration.
 *
 *   3. No past bookings
 *      Each field is already annotated @Future — if validation reaches this
 *      validator both times are in the future. This rule is recorded here as
 *      a comment so the intent is explicit in the codebase.
 *
 * TODO: Extend this validator to enforce a minimum booking duration
 *       (e.g. at least 15 minutes) and a maximum duration (e.g. 8 hours).
 */
public class BookingTimeValidator
        implements ConstraintValidator<ValidBookingTime, CreateBookingRequest> {

    @Override
    public void initialize(ValidBookingTime annotation) {
        // No initialisation needed
    }

    @Override
    public boolean isValid(CreateBookingRequest request,
                           ConstraintValidatorContext context) {

        // Rule 1: skip if either field is null — @NotNull on the fields handles that
        if (request.getStartTime() == null || request.getEndTime() == null) {
            return true;
        }

        // Rule 2: startTime must be strictly before endTime
        boolean valid = request.getStartTime().isBefore(request.getEndTime());

        if (!valid) {
            // Override the default annotation-level message with a field-specific one
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                            "startTime must be before endTime")
                    .addPropertyNode("startTime")
                    .addConstraintViolation();
        }

        return valid;
    }
}
