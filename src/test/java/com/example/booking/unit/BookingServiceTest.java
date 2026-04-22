package com.example.booking.unit;

import com.example.booking.domain.entity.Booking;
import com.example.booking.domain.entity.Resource;
import com.example.booking.domain.entity.User;
import com.example.booking.domain.enums.BookingStatus;
import com.example.booking.dto.BookingResponse;
import com.example.booking.dto.CreateBookingRequest;
import com.example.booking.exception.BookingNotFoundException;
import com.example.booking.exception.IllegalStateTransitionException;
import com.example.booking.exception.ResourceUnavailableException;
import com.example.booking.mapper.BookingMapper;
import com.example.booking.repository.BookingRepository;
import com.example.booking.repository.ResourceRepository;
import com.example.booking.repository.UserRepository;
import com.example.booking.service.impl.BookingServiceImpl;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private UserRepository userRepository;
    @Mock private ResourceRepository resourceRepository;
    @Mock private BookingMapper bookingMapper;
    @Mock private ApplicationEventPublisher eventPublisher;

    private BookingServiceImpl bookingService;

    private UUID userId;
    private UUID resourceId;
    private UUID bookingId;
    private User mockUser;
    private Resource mockResource;
    private Booking mockBooking;

    @BeforeEach
    void setUp() {
        bookingService = new BookingServiceImpl(
                bookingRepository, userRepository, resourceRepository,
                bookingMapper, eventPublisher, new SimpleMeterRegistry());

        userId = UUID.randomUUID();
        resourceId = UUID.randomUUID();
        bookingId = UUID.randomUUID();
        mockUser = new User("Alice", "alice@example.com");
        mockResource = new Resource("Conference Room A", "10-person room", 10);
        mockBooking = new Booking(mockUser, mockResource,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(1));
    }

    // ─── getBooking ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getBooking: returns mapped response when booking exists")
    void getBooking_found() {
        BookingResponse expected = BookingResponse.builder()
                .id(bookingId).userId(userId).resourceId(resourceId)
                .status(BookingStatus.PENDING).build();
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(mockBooking));
        when(bookingMapper.toResponse(mockBooking)).thenReturn(expected);

        BookingResponse result = bookingService.getBooking(bookingId);

        assertThat(result).isEqualTo(expected);
        verify(bookingRepository).findById(bookingId);
    }

    @Test
    @DisplayName("getBooking: throws BookingNotFoundException when not found")
    void getBooking_notFound() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.getBooking(bookingId))
                .isInstanceOf(BookingNotFoundException.class)
                .hasMessageContaining(bookingId.toString());
    }

    // ─── createBooking ───────────────────────────────────────────────────────

    @Test
    @DisplayName("createBooking: saves PENDING booking when slot is free")
    void createBooking_success() {
        CreateBookingRequest request = new CreateBookingRequest(
                userId, resourceId,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(2));
        BookingResponse expected = BookingResponse.builder()
                .id(bookingId).userId(userId).resourceId(resourceId)
                .status(BookingStatus.PENDING).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(mockResource));
        when(bookingRepository.findOverlappingBookings(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenReturn(mockBooking);
        when(bookingMapper.toResponse(mockBooking)).thenReturn(expected);

        BookingResponse result = bookingService.createBooking(request);

        assertThat(result).isEqualTo(expected);
        verify(bookingRepository).saveAndFlush(any(Booking.class));
    }

    @Test
    @DisplayName("createBooking: throws ResourceUnavailableException on overlap")
    void createBooking_overlap() {
        CreateBookingRequest request = new CreateBookingRequest(
                userId, resourceId,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(2));

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(mockResource));
        when(bookingRepository.findOverlappingBookings(any(), any(), any()))
                .thenReturn(List.of(mockBooking));

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(ResourceUnavailableException.class);
        verify(bookingRepository, never()).save(any());
    }

    // ─── confirmBooking ──────────────────────────────────────────────────────

    @Test
    @DisplayName("confirmBooking: PENDING → CONFIRMED succeeds")
    void confirmBooking_pendingToConfirmed() {
        assertThat(mockBooking.getStatus()).isEqualTo(BookingStatus.PENDING);
        BookingResponse expected = BookingResponse.builder()
                .id(bookingId).status(BookingStatus.CONFIRMED).build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(mockBooking));
        when(bookingRepository.save(mockBooking)).thenReturn(mockBooking);
        when(bookingMapper.toResponse(mockBooking)).thenReturn(expected);

        BookingResponse result = bookingService.confirmBooking(bookingId);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    @DisplayName("confirmBooking: throws IllegalStateTransitionException when already CONFIRMED")
    void confirmBooking_alreadyConfirmed() {
        mockBooking.confirm();  // move to CONFIRMED

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(mockBooking));

        assertThatThrownBy(() -> bookingService.confirmBooking(bookingId))
                .isInstanceOf(IllegalStateTransitionException.class)
                .hasMessageContaining("CONFIRMED");
    }

    @Test
    @DisplayName("confirmBooking: throws IllegalStateTransitionException when CANCELLED")
    void confirmBooking_cancelled() {
        mockBooking.cancel();  // move to CANCELLED

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(mockBooking));

        assertThatThrownBy(() -> bookingService.confirmBooking(bookingId))
                .isInstanceOf(IllegalStateTransitionException.class)
                .hasMessageContaining("CANCELLED");
    }

    @Test
    @DisplayName("confirmBooking: throws BookingNotFoundException for unknown id")
    void confirmBooking_notFound() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.confirmBooking(bookingId))
                .isInstanceOf(BookingNotFoundException.class);
    }

    // ─── cancelBooking ───────────────────────────────────────────────────────

    @Test
    @DisplayName("cancelBooking: PENDING → CANCELLED succeeds")
    void cancelBooking_pendingToCancelled() {
        assertThat(mockBooking.getStatus()).isEqualTo(BookingStatus.PENDING);
        BookingResponse expected = BookingResponse.builder()
                .id(bookingId).status(BookingStatus.CANCELLED).build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(mockBooking));
        when(bookingRepository.save(mockBooking)).thenReturn(mockBooking);
        when(bookingMapper.toResponse(mockBooking)).thenReturn(expected);

        BookingResponse result = bookingService.cancelBooking(bookingId);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    @DisplayName("cancelBooking: CONFIRMED → CANCELLED succeeds")
    void cancelBooking_confirmedToCancelled() {
        mockBooking.confirm();  // move to CONFIRMED
        assertThat(mockBooking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        BookingResponse expected = BookingResponse.builder()
                .id(bookingId).status(BookingStatus.CANCELLED).build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(mockBooking));
        when(bookingRepository.save(mockBooking)).thenReturn(mockBooking);
        when(bookingMapper.toResponse(mockBooking)).thenReturn(expected);

        BookingResponse result = bookingService.cancelBooking(bookingId);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    @DisplayName("cancelBooking: throws IllegalStateTransitionException if already CANCELLED")
    void cancelBooking_alreadyCancelled() {
        mockBooking.cancel();  // put it into CANCELLED state
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(mockBooking));

        assertThatThrownBy(() -> bookingService.cancelBooking(bookingId))
                .isInstanceOf(IllegalStateTransitionException.class)
                .hasMessageContaining("already cancelled");
    }

    @Test
    @DisplayName("cancelBooking: throws BookingNotFoundException for unknown id")
    void cancelBooking_notFound() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.cancelBooking(bookingId))
                .isInstanceOf(BookingNotFoundException.class);
    }
}
