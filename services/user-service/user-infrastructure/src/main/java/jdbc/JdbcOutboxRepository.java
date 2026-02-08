package jdbc;

import org.dimitri.user.domain.OutboxEvent;
import ports.OutboxReadPort;
import ports.OutboxWritePort;
import persistence.OutboxRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcOutboxRepository implements OutboxRepository, OutboxWritePort, OutboxReadPort {

    private final JdbcTemplate jdbc;

    public JdbcOutboxRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ✅ Port métier
    @Override
    public void save(OutboxEvent event) {
        insert(event);
    }

    // ✅ Interface infra
    @Override
    public void insert(OutboxEvent event) {
        jdbc.update("""
            INSERT INTO outbox_events(
                id,
                aggregate_type,
                aggregate_id,
                event_type,
                payload,
                status,
                created_at,
                attempts,
                next_attempt_at,
                last_error
            )
            VALUES (?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?)
        """,
                event.id(),
                event.aggregateType(),
                event.aggregateId(),
                event.eventType(),
                event.payload(), // JSON string
                event.status().name(),
                Timestamp.from(event.createdAt()),
                event.attempts(),
                event.nextAttemptAt() == null ? null : Timestamp.from(event.nextAttemptAt()),
                event.lastError()
        );
    }

    /**
     * Récupère des events à publier.
     * Recommandation: appeler cette méthode dans une transaction
     * et publier ensuite, puis markSent/markFailed.
     *
     * Version robuste PostgreSQL: FOR UPDATE SKIP LOCKED
     * => évite que 2 workers prennent les mêmes lignes.
     */
    @Override
    @Transactional(readOnly = true)
    public List<OutboxEvent> findPending(int limit) {
        Instant now = Instant.now();

        return jdbc.query("""
            SELECT *
            FROM outbox_events
            WHERE status = 'PENDING'
              AND (next_attempt_at IS NULL OR next_attempt_at <= ?)
            ORDER BY created_at ASC
            LIMIT ?
            FOR UPDATE SKIP LOCKED
        """, outboxRowMapper(), Timestamp.from(now), limit);
    }

    @Override
    public void markSent(UUID id) {
        jdbc.update("""
            UPDATE outbox_events
            SET status = 'SENT',
                last_error = NULL
            WHERE id = ?
        """, id);
    }

    @Override
    public void markFailed(UUID id, String error) {
        // stratégie: retry exponentiel simple : 5s * 2^attempts (cap 5 min)
        // attempts est incrémenté à chaque échec
        // next_attempt_at est calculé pour retenter plus tard

        // 1) lire attempts courants
        Integer attempts = jdbc.queryForObject("""
            SELECT attempts
            FROM outbox_events
            WHERE id = ?
        """, Integer.class, id);

        int nextAttempts = (attempts == null ? 0 : attempts) + 1;

        long delaySeconds = Math.min(300, (long) (5 * Math.pow(2, Math.max(0, nextAttempts - 1))));
        Instant nextAttemptAt = Instant.now().plusSeconds(delaySeconds);

        jdbc.update("""
            UPDATE outbox_events
            SET status = 'PENDING',
                attempts = ?,
                next_attempt_at = ?,
                last_error = ?
            WHERE id = ?
        """,
                nextAttempts,
                Timestamp.from(nextAttemptAt),
                truncate(error, 2000),
                id
        );
    }

    // Optionnel (utile pour debug / tests)
    @Override
    public Optional<OutboxEvent> findById(UUID id) {
        List<OutboxEvent> rows = jdbc.query("""
            SELECT *
            FROM outbox_events
            WHERE id = ?
        """, outboxRowMapper(), id);

        return rows.stream().findFirst();
    }

    private RowMapper<OutboxEvent> outboxRowMapper() {
        return new RowMapper<>() {
            @Override
            public OutboxEvent mapRow(ResultSet rs, int rowNum) throws SQLException {
                return new OutboxEvent(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("aggregate_type"),
                        rs.getString("aggregate_id"),
                        rs.getString("event_type"),
                        rs.getString("payload"),
                        OutboxEvent.Status.valueOf(rs.getString("status")),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getInt("attempts"),
                        rs.getTimestamp("next_attempt_at") == null ? null : rs.getTimestamp("next_attempt_at").toInstant(),
                        rs.getString("last_error")
                );
            }
        };
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        if (s.length() <= max) return s;
        return s.substring(0, max);
    }
}


