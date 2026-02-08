package jdbc;

import org.dimitri.user.domain.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import persistence.UserRepository;
import ports.UserWritePort;

import java.sql.Timestamp;

@Repository
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

