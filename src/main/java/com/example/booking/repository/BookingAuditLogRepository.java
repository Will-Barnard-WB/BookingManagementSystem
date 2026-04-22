package com.example.booking.repository;

import com.example.booking.domain.entity.BookingAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BookingAuditLogRepository extends JpaRepository<BookingAuditLog, UUID> {

    List<BookingAuditLog> findByBookingIdOrderByChangedAtAsc(UUID bookingId);
}
