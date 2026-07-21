package org.dimitri.shared.outbox;

import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public abstract class AbstractJdbcOutboxRepository implements OutboxPort {
    private final JdbcTemplate jdbc;
    private final OutboxRetryPolicy retryPolicy;
    private final String producer;

    protected AbstractJdbcOutboxRepository(JdbcTemplate jdbc, OutboxRetryPolicy retryPolicy, String producer) {
        this.jdbc = jdbc;
        this.retryPolicy = retryPolicy;
        this.producer = producer;
    }

    @Override
    public void save(OutboxEvent event) {
        jdbc.update("""
            INSERT INTO outbox_events(
                id, producer, aggregate_type, aggregate_id, event_type, payload,
                status, created_at, attempts, next_attempt_at, last_error
            ) VALUES (?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?)
        """, event.id(), producer, event.aggregateType(), event.aggregateId(), event.eventType(), event.payload(),
                event.status().name(), Timestamp.from(event.createdAt()), event.retryCount(),
                timestamp(event.nextAttemptAt()), event.lastError());
    }

    @Override
    public List<OutboxEvent> findPending(int limit) {
        return jdbc.query("""
            SELECT id, aggregate_type, aggregate_id, event_type, payload, status,
                   created_at, attempts, next_attempt_at, last_error
            FROM outbox_events
            WHERE producer = ? AND status = 'PENDING'
              AND (next_attempt_at IS NULL OR next_attempt_at <= NOW())
            ORDER BY created_at
            LIMIT ?
        """, (rs, rowNum) -> new OutboxEvent(
                rs.getObject("id", UUID.class), rs.getString("aggregate_type"), rs.getString("aggregate_id"),
                rs.getString("event_type"), rs.getString("payload"),
                OutboxEvent.Status.valueOf(rs.getString("status")), rs.getTimestamp("created_at").toInstant(),
                rs.getInt("attempts"), instant(rs.getTimestamp("next_attempt_at")), rs.getString("last_error")
        ), producer, limit);
    }

    @Override
    public void markSent(UUID eventId) {
        jdbc.update("UPDATE outbox_events SET status = 'SENT', last_error = NULL WHERE id = ? AND producer = ?",
                eventId, producer);
    }

    @Override
    public void markFailed(UUID eventId, String error) {
        Integer attempts = jdbc.queryForObject(
                "SELECT attempts FROM outbox_events WHERE id = ? AND producer = ?", Integer.class, eventId, producer);
        int nextAttempt = (attempts == null ? 0 : attempts) + 1;
        Instant retryAt = Instant.now().plus(retryPolicy.delayForAttempt(nextAttempt));
        jdbc.update("""
            UPDATE outbox_events
            SET status = 'PENDING', attempts = ?, next_attempt_at = ?, last_error = ?
            WHERE id = ? AND producer = ?
        """, nextAttempt, Timestamp.from(retryAt), truncate(error, 2000), eventId, producer);
    }

    private static Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    private static Instant instant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }

    private static String truncate(String value, int maximumLength) {
        if (value == null || value.length() <= maximumLength) return value;
        return value.substring(0, maximumLength);
    }
}
