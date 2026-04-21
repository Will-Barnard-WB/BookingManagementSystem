package com.example.booking.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Class-level constraint annotation applied to CreateBookingRequest.
 *
 * Enforces two rules that cannot be expressed with standard annotations alone:
 *   1. startTime must be strictly before endTime
 *   2. Neither startTime nor endTime may be in the past
 *      (the @Future annotation on each field handles the individual past-check;
 *       this validator adds the inter-field ordering check)
 *
 * Usage:
 *   @ValidBookingTime
 *   public class CreateBookingRequest { ... }
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = BookingTimeValidator.class)
public @interface ValidBookingTime {

    String message() default "startTime must be before endTime";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
