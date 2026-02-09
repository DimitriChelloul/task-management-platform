package org.dimitri.user.infrastructure.jdbc;

import org.dimitri.user.domain.User;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.dimitri.user.infrastructure.persistence.UserRepository;
import org.dimitri.user.application.ports.UserWritePort;

import java.sql.Timestamp;

@Repository
@Profile("jdbc")

public class JdbcUserRepository
        implements UserRepository, UserWritePort {

    private final JdbcTemplate jdbc;

    public JdbcUserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void insert(User user) {
        jdbc.update("""
            INSERT INTO users(id, email, created_at)
            VALUES (?, ?, ?)
        """, user.id(), user.email(), Timestamp.from(user.createdAt()));
    }

    // Adapter vers le port métier
    @Override
    public void save(User user) {
        insert(user);
    }
}

