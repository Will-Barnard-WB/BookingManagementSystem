package com.example.booking.mapper;

import com.example.booking.domain.entity.Booking;
import com.example.booking.dto.BookingResponse;
import org.springframework.stereotype.Component;

/**
 * Manual mapper for Booking entities.
 *
 * Note: accessing booking.getUser() and booking.getResource() will trigger
 * lazy loads if the session is still open (e.g. inside a @Transactional method
 * or via Open-Session-in-View). Ensure the entity is properly initialised
 * before calling this mapper from the service layer.
 */
@Component
public class BookingMapper {

    public BookingResponse toResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .userId(booking.getUser().getId())
                .userName(booking.getUser().getName())
                .resourceId(booking.getResource().getId())
                .resourceName(booking.getResource().getName())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .status(booking.getStatus())
                .createdAt(booking.getCreatedAt())
                .build();
    }
}
