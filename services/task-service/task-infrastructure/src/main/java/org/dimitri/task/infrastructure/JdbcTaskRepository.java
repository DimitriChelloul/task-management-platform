package org.dimitri.task.infrastructure;

import org.dimitri.task.application.TaskRepository;
import org.dimitri.task.domain.Task;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcTaskRepository implements TaskRepository {
    private final JdbcTemplate jdbc;

    public JdbcTaskRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean save(Task task) {
        return jdbc.update("""
            INSERT INTO tasks(id, title, status, source_user_id, created_at, completed_at)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT (source_user_id) DO NOTHING
        """, task.id(), task.title(), task.status().name(), task.sourceUserId(),
                Timestamp.from(task.createdAt()), null) == 1;
    }

    @Override
    public List<Task> findAll() {
        return jdbc.query("SELECT * FROM tasks ORDER BY created_at DESC", this::map);
    }

    @Override
    public Optional<Task> findById(UUID id) {
        return jdbc.query("SELECT * FROM tasks WHERE id = ?", this::map, id).stream().findFirst();
    }

    @Override
    public boolean markCompleted(UUID id, Instant completedAt) {
        return jdbc.update("""
            UPDATE tasks SET status = 'COMPLETED', completed_at = ?
            WHERE id = ? AND status = 'OPEN'
        """, Timestamp.from(completedAt), id) == 1;
    }

    private Task map(ResultSet rs, int rowNumber) throws SQLException {
        Timestamp completedAt = rs.getTimestamp("completed_at");
        return new Task(rs.getObject("id", UUID.class), rs.getString("title"),
                Task.Status.valueOf(rs.getString("status")), rs.getString("source_user_id"),
                rs.getTimestamp("created_at").toInstant(), completedAt == null ? null : completedAt.toInstant());
    }
}
