package com.example.booking.unit;

import com.example.booking.dto.CreateBookingRequest;
import com.example.booking.validation.BookingTimeValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BookingTimeValidator.
 *
 * No Spring context needed — we instantiate the validator directly.
 * ConstraintValidatorContext is mocked to avoid building a full Validator instance.
 */
class BookingValidatorTest {

    private BookingTimeValidator validator;
    private ConstraintValidatorContext context;
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;
    private ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext nodeBuilder;

    private final LocalDateTime future = LocalDateTime.now().plusDays(1);

    @BeforeEach
    void setUp() {
        validator = new BookingTimeValidator();

        // Stub the context chain so we can call disableDefaultConstraintViolation()
        // and buildConstraintViolationWithTemplate() without NPEs.
        context = mock(ConstraintValidatorContext.class);
        violationBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        nodeBuilder = mock(
                ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext.class);

        when(context.buildConstraintViolationWithTemplate(anyString()))
                .thenReturn(violationBuilder);
        when(violationBuilder.addPropertyNode(anyString()))
                .thenReturn(nodeBuilder);
    }

    @Test
    @DisplayName("valid when startTime is before endTime")
    void valid_startBeforeEnd() {
        CreateBookingRequest request = new CreateBookingRequest(
                UUID.randomUUID(), UUID.randomUUID(),
                future, future.plusHours(2));

        assertThat(validator.isValid(request, context)).isTrue();
    }

    @Test
    @DisplayName("invalid when startTime equals endTime")
    void invalid_startEqualsEnd() {
        CreateBookingRequest request = new CreateBookingRequest(
                UUID.randomUUID(), UUID.randomUUID(),
                future, future);   // zero-duration booking

        assertThat(validator.isValid(request, context)).isFalse();
    }

    @Test
    @DisplayName("invalid when startTime is after endTime")
    void invalid_startAfterEnd() {
        CreateBookingRequest request = new CreateBookingRequest(
                UUID.randomUUID(), UUID.randomUUID(),
                future.plusHours(3), future);   // end before start

        assertThat(validator.isValid(request, context)).isFalse();
    }

    @Test
    @DisplayName("skips validation when startTime is null (deferred to @NotNull)")
    void skips_whenStartTimeNull() {
        CreateBookingRequest request = new CreateBookingRequest(
                UUID.randomUUID(), UUID.randomUUID(),
                null, future);

        assertThat(validator.isValid(request, context)).isTrue();
    }

    @Test
    @DisplayName("skips validation when endTime is null (deferred to @NotNull)")
    void skips_whenEndTimeNull() {
        CreateBookingRequest request = new CreateBookingRequest(
                UUID.randomUUID(), UUID.randomUUID(),
                future, null);

        assertThat(validator.isValid(request, context)).isTrue();
    }
}
