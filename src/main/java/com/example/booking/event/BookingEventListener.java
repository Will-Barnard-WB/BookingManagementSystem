package com.example.booking.event;

import com.example.booking.domain.entity.BookingAuditLog;
import com.example.booking.domain.event.BookingCancelledEvent;
import com.example.booking.domain.event.BookingConfirmedEvent;
import com.example.booking.domain.event.BookingCreatedEvent;
import com.example.booking.repository.BookingAuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Handles booking domain events.
 *
 * AUDIT listeners (BEFORE_COMMIT):
 *   Run inside the booking's own transaction — if the transaction rolls back,
 *   the audit row rolls back too (transactional outbox pattern).
 *
 * NOTIFICATION listeners (AFTER_COMMIT):
 *   Run only once the booking has committed, so we never notify about a booking
 *   that later rolled back. They currently write a log line; an email or push
 *   integration would hook in at the same point.
 */
@Component
public class BookingEventListener {

    private static final Logger log = LoggerFactory.getLogger(BookingEventListener.class);

    private final BookingAuditLogRepository auditLogRepository;

    public BookingEventListener(BookingAuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    // ── Audit listeners (transactional outbox) ────────────────────────────

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void auditBookingCreated(BookingCreatedEvent event) {
        auditLogRepository.save(new BookingAuditLog(
                event.getBookingId(), event.getUserId(), event.getResourceId(),
                null, event.getStatus(), event.getTimestamp()));
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void auditBookingConfirmed(BookingConfirmedEvent event) {
        auditLogRepository.save(new BookingAuditLog(
                event.getBookingId(), event.getUserId(), event.getResourceId(),
                event.getPreviousStatus(), event.getStatus(), event.getTimestamp()));
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void auditBookingCancelled(BookingCancelledEvent event) {
        auditLogRepository.save(new BookingAuditLog(
                event.getBookingId(), event.getUserId(), event.getResourceId(),
                event.getPreviousStatus(), event.getStatus(), event.getTimestamp()));
    }

    // ── Notification listeners (post-commit) ──────────────────────────────

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notifyBookingCreated(BookingCreatedEvent event) {
        log.info("[NOTIFY] Booking {} created — user={} resource={}",
                event.getBookingId(), event.getUserId(), event.getResourceId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notifyBookingConfirmed(BookingConfirmedEvent event) {
        log.info("[NOTIFY] Booking {} confirmed — user={} resource={}",
                event.getBookingId(), event.getUserId(), event.getResourceId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notifyBookingCancelled(BookingCancelledEvent event) {
        log.info("[NOTIFY] Booking {} cancelled — user={} resource={}",
                event.getBookingId(), event.getUserId(), event.getResourceId());
    }
}
