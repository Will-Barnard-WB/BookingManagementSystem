package com.example.booking.unit;

import com.example.booking.domain.entity.Booking;
import com.example.booking.domain.entity.Resource;
import com.example.booking.domain.entity.User;
import com.example.booking.domain.enums.BookingStatus;
import com.example.booking.dto.BookingResponse;
import com.example.booking.exception.BookingNotFoundException;
import com.example.booking.exception.InvalidBookingException;
import com.example.booking.mapper.BookingMapper;
import com.example.booking.repository.BookingRepository;
import com.example.booking.repository.ResourceRepository;
import com.example.booking.repository.UserRepository;
import com.example.booking.service.impl.BookingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BookingServiceImpl.
 *
 * All dependencies are mocked with Mockito so no Spring context or database
 * is required. Tests run in milliseconds.
 *
 * TODO: fill in the @Test bodies as business logic is implemented.
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private UserRepository userRepository;
    @Mock private ResourceRepository resourceRepository;
    @Mock private BookingMapper bookingMapper;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private UUID userId;
    private UUID resourceId;
    private UUID bookingId;
    private User mockUser;
    private Resource mockResource;
    private Booking mockBooking;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        resourceId = UUID.randomUUID();
        bookingId = UUID.randomUUID();
        mockUser = new User("Alice", "alice@example.com");
        mockResource = new Resource("Conference Room A", "10-person room", 10);
        mockBooking = new Booking(mockUser, mockResource,
                java.time.LocalDateTime.now().plusDays(1),
                java.time.LocalDateTime.now().plusDays(1).plusHours(1));
    }

    // ─── getBooking ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getBooking: returns response when booking exists")
    void getBooking_found() {
        // TODO:
        // given
        //   bookingRepository.findById(bookingId) returns Optional.of(mockBooking)
        //   bookingMapper.toResponse(mockBooking) returns a BookingResponse
        // when
        //   bookingService.getBooking(bookingId)
        // then
        //   the mapped response is returned
    }

    @Test
    @DisplayName("getBooking: throws BookingNotFoundException when not found")
    void getBooking_notFound() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.getBooking(bookingId))
                .isInstanceOf(BookingNotFoundException.class)
                .hasMessageContaining(bookingId.toString());
    }

    // ─── cancelBooking ───────────────────────────────────────────────────────

    @Test
    @DisplayName("cancelBooking: throws BookingNotFoundException for unknown id")
    void cancelBooking_notFound() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.cancelBooking(bookingId))
                .isInstanceOf(BookingNotFoundException.class);
    }

    @Test
    @DisplayName("cancelBooking: throws InvalidBookingException if already cancelled")
    void cancelBooking_alreadyCancelled() {
        mockBooking.cancel();   // put it into CANCELLED state
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(mockBooking));

        assertThatThrownBy(() -> bookingService.cancelBooking(bookingId))
                .isInstanceOf(InvalidBookingException.class)
                .hasMessageContaining("already cancelled");
    }

    // ─── createBooking ───────────────────────────────────────────────────────

    @Test
    @DisplayName("createBooking: TODO — happy path, no overlaps")
    void createBooking_success() {
        // TODO:
        // given
        //   user and resource both exist
        //   bookingRepository.findOverlappingBookings returns empty list
        // when
        //   bookingService.createBooking(request)
        // then
        //   a PENDING booking is saved and the response is returned
    }

    @Test
    @DisplayName("createBooking: TODO — throws ResourceUnavailableException on overlap")
    void createBooking_overlap() {
        // TODO:
        // given
        //   user and resource both exist
        //   bookingRepository.findOverlappingBookings returns a non-empty list
        // when
        //   bookingService.createBooking(request)
        // then
        //   ResourceUnavailableException is thrown
    }
}
