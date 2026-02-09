package org.dimitri.user.infrastructure.outbox;

import org.dimitri.user.application.ports.OutboxReadPort;
import org.dimitri.user.domain.OutboxEvent;
import org.dimitri.user.domain.OutboxEvent.Status;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcOutboxReadAdapter implements OutboxReadPort {

    private final JdbcTemplate jdbcTemplate;

    public JdbcOutboxReadAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<OutboxEvent> fetchBatch(int batchSize) {
        String sql = """
            SELECT
              id,
              aggregate_type,
              aggregate_id,
              event_type,
              payload,
              status,
              created_at,
              retry_count,
              next_attempt_at,
              last_error
            FROM outbox
            WHERE status = 'PENDING'
              AND (next_attempt_at IS NULL OR next_attempt_at <= NOW())
            ORDER BY created_at
            LIMIT ?
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            UUID id = rs.getObject("id", UUID.class);

            String aggregateType = rs.getString("aggregate_type");
            String aggregateId = rs.getString("aggregate_id");
            String eventType = rs.getString("event_type");
            String payload = rs.getString("payload");

            // status stocké en DB comme texte "PENDING", "SENT", ...
            Status status = Status.valueOf(rs.getString("status"));

            Instant createdAt = rs.getTimestamp("created_at").toInstant();

            int retryCount = rs.getInt("retry_count");

            Timestamp nextAttemptTs = rs.getTimestamp("next_attempt_at");
            Instant nextAttemptAt = (nextAttemptTs != null) ? nextAttemptTs.toInstant() : null;

            String lastError = rs.getString("last_error"); // peut être null

            return new OutboxEvent(
                    id,
                    aggregateType,
                    aggregateId,
                    eventType,
                    payload,
                    status,
                    createdAt,
                    retryCount,
                    nextAttemptAt,
                    lastError
            );
        }, batchSize);
    }

    @Override
    public List<OutboxEvent> findPending(int limit) {
        return List.of();
    }

    @Override
    public void markSent(UUID eventId) {

    }

    @Override
    public void markFailed(UUID eventId, String error) {

    }
}
