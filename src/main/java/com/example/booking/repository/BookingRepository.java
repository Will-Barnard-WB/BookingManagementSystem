package com.example.booking.repository;

import com.example.booking.domain.entity.Booking;
import com.example.booking.domain.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    Optional<Booking> findById(UUID id);

    List<Booking> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Booking> findByResourceIdAndStatusNot(UUID resourceId, BookingStatus status);

    /**
     * Overlap detection query — the heart of double-booking prevention.
     *
     * Two intervals [startA, endA) and [startB, endB) overlap when:
     *     startA < endB  AND  startB < endA
     *
     * Returns any non-CANCELLED bookings for the given resource whose
     * time window intersects with [startTime, endTime).
     * If this list is non-empty, throw ResourceUnavailableException.
     */
    @Query("""
            SELECT b FROM Booking b
            WHERE b.resource.id = :resourceId
              AND b.status <> 'CANCELLED'
              AND b.startTime < :endTime
              AND b.endTime > :startTime
            """)
    List<Booking> findOverlappingBookings(
            @Param("resourceId") UUID resourceId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
