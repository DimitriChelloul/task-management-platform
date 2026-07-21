package org.dimitri.user.infrastructure.jdbc;

import org.dimitri.user.domain.User;
import org.dimitri.user.application.EmailAlreadyUsedException;
import org.dimitri.user.application.UserRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcUserRepository implements UserRepository {

    private final JdbcTemplate jdbc;

    public JdbcUserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(User user) {
        try {
            jdbc.update("""
                INSERT INTO users(id, email, created_at)
                VALUES (?, ?, ?)
            """, user.id(), user.email(), Timestamp.from(user.createdAt()));
        } catch (DuplicateKeyException exception) {
            throw new EmailAlreadyUsedException(user.email());
        }
    }

    @Override
    public List<User> findAll() {
        return jdbc.query("SELECT id, email, created_at FROM users ORDER BY created_at DESC", this::map);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jdbc.query("SELECT id, email, created_at FROM users WHERE id = ?", this::map, id)
                .stream().findFirst();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jdbc.query("SELECT id, email, created_at FROM users WHERE LOWER(email) = ?", this::map, email)
                .stream().findFirst();
    }

    private User map(ResultSet result, int rowNumber) throws SQLException {
        return new User(result.getObject("id", UUID.class), result.getString("email"),
                result.getTimestamp("created_at").toInstant());
    }

}

