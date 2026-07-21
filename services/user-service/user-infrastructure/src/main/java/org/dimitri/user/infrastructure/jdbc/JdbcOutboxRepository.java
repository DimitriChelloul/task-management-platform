package org.dimitri.user.infrastructure.jdbc;

import org.dimitri.shared.outbox.AbstractJdbcOutboxRepository;
import org.dimitri.shared.outbox.OutboxRetryPolicy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcOutboxRepository extends AbstractJdbcOutboxRepository {
    public JdbcOutboxRepository(JdbcTemplate jdbc, OutboxRetryPolicy retryPolicy) {
        super(jdbc, retryPolicy, "user-service");
    }
}


