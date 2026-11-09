package app.todo.todo;

import app.todo.common.UtcTimestamps;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * {@link TodoRepository} over {@code JdbcClient} (ADR-0009). All SQL names only {@code todos}. Search is a literal
 * substring test on the derived lower-case columns ({@code instr}), so {@code %}, {@code _}, {@code \} and quotes match
 * themselves (ADR-0015). Timestamps are written through {@link UtcTimestamps} (ADR-0017).
 */
@Component
class JdbcTodoRepository implements TodoRepository {

    private static final String COLUMNS =
            "public_id, title, description, due_date, priority, done, created_at, updated_at";

    /** updated_at = max(:now, previous + 1 ms), computed in the same statement as the write (sdd.md section 7). */
    private static final String ADVANCE_UPDATED_AT = "updated_at = CASE WHEN :now > updated_at THEN :now "
            + "ELSE strftime('%Y-%m-%dT%H:%M:%fZ', updated_at, '+0.001 seconds') END";

    private static final RowMapper<Todo> ROW = (rs, rowNum) -> new Todo(
            rs.getString("public_id"),
            rs.getString("title"),
            rs.getString("description"),
            rs.getString("due_date"),
            Priority.valueOf(rs.getString("priority")),
            rs.getInt("done") == 1,
            rs.getString("created_at"),
            rs.getString("updated_at"));

    private final JdbcClient jdbc;

    JdbcTodoRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Todo> findByPublicId(long userId, String publicId) {
        return jdbc.sql("SELECT " + COLUMNS + " FROM todos WHERE public_id = :publicId AND user_id = :userId")
                .param("publicId", publicId)
                .param("userId", userId)
                .query(ROW)
                .optional();
    }

    @Override
    public TodoPageResult list(long userId, TodoQuery query) {
        StringBuilder where = new StringBuilder("user_id = :userId");
        switch (query.status()) {
            case ACTIVE -> where.append(" AND done = 0");
            case COMPLETED -> where.append(" AND done = 1");
            case ALL -> { }
        }
        boolean searching = !query.search().isEmpty();
        if (searching) {
            where.append(" AND (instr(title_search, :q) > 0 OR instr(description_search, :q) > 0)");
        }

        long totalItems = bind(jdbc.sql("SELECT count(*) FROM todos WHERE " + where), userId, query, searching)
                .query(Long.class)
                .single();
        int totalPages = (int) ((totalItems + TodoQuery.PAGE_SIZE - 1) / TodoQuery.PAGE_SIZE);
        long page = totalItems == 0 ? 1 : Math.min(query.page(), totalPages);
        long offset = (page - 1) * TodoQuery.PAGE_SIZE;

        List<Todo> items = bind(jdbc.sql("SELECT " + COLUMNS + " FROM todos WHERE " + where
                        + " ORDER BY created_at DESC, id DESC LIMIT :limit OFFSET :offset"), userId, query, searching)
                .param("limit", TodoQuery.PAGE_SIZE)
                .param("offset", offset)
                .query(ROW)
                .list();
        return new TodoPageResult(items, (int) page, totalItems, totalPages);
    }

    private static JdbcClient.StatementSpec bind(JdbcClient.StatementSpec spec, long userId, TodoQuery query, boolean searching) {
        spec = spec.param("userId", userId);
        return searching ? spec.param("q", query.search()) : spec;
    }

    @Override
    public TodoTotals totals(long userId) {
        return jdbc.sql("SELECT count(*) AS total, coalesce(sum(done), 0) AS completed FROM todos WHERE user_id = :userId")
                .param("userId", userId)
                .query((rs, rowNum) -> {
                    long all = rs.getLong("total");
                    long completed = rs.getLong("completed");
                    return new TodoTotals(all, all - completed, completed);
                })
                .single();
    }

    @Override
    public Todo insert(long userId, NewTodo todo) {
        TodoContent content = todo.content();
        String publicId = UUID.randomUUID().toString();
        String timestamp = UtcTimestamps.format(todo.createdAt());
        jdbc.sql("INSERT INTO todos (public_id, user_id, title, title_search, description, description_search, "
                        + "due_date, priority, done, created_at, updated_at) "
                        + "VALUES (:publicId, :userId, :title, :titleSearch, :description, :descriptionSearch, "
                        + ":dueDate, :priority, 0, :timestamp, :timestamp)")
                .param("publicId", publicId)
                .param("userId", userId)
                .param("title", content.title())
                .param("titleSearch", lower(content.title()))
                .param("description", content.description())
                .param("descriptionSearch", content.description() == null ? null : lower(content.description()))
                .param("dueDate", content.dueDate())
                .param("priority", content.priority().name())
                .param("timestamp", timestamp)
                .update();
        return new Todo(publicId, content.title(), content.description(), content.dueDate(), content.priority(), false,
                timestamp, timestamp);
    }

    @Override
    public boolean updateContent(long userId, String publicId, TodoContent content, Instant updatedAt) {
        int changed = jdbc.sql("UPDATE todos SET title = :title, title_search = :titleSearch, description = :description, "
                        + "description_search = :descriptionSearch, due_date = :dueDate, priority = :priority, "
                        + ADVANCE_UPDATED_AT + " WHERE public_id = :publicId AND user_id = :userId")
                .param("title", content.title())
                .param("titleSearch", lower(content.title()))
                .param("description", content.description())
                .param("descriptionSearch", content.description() == null ? null : lower(content.description()))
                .param("dueDate", content.dueDate())
                .param("priority", content.priority().name())
                .param("now", UtcTimestamps.format(updatedAt))
                .param("publicId", publicId)
                .param("userId", userId)
                .update();
        return changed == 1;
    }

    @Override
    public boolean updateDone(long userId, String publicId, boolean done, Instant updatedAt) {
        int changed = jdbc.sql("UPDATE todos SET done = :done, " + ADVANCE_UPDATED_AT
                        + " WHERE public_id = :publicId AND user_id = :userId")
                .param("done", done ? 1 : 0)
                .param("now", UtcTimestamps.format(updatedAt))
                .param("publicId", publicId)
                .param("userId", userId)
                .update();
        return changed == 1;
    }

    @Override
    public boolean delete(long userId, String publicId) {
        return jdbc.sql("DELETE FROM todos WHERE public_id = :publicId AND user_id = :userId")
                .param("publicId", publicId)
                .param("userId", userId)
                .update() == 1;
    }

    @Override
    public int deleteCompleted(long userId) {
        return jdbc.sql("DELETE FROM todos WHERE user_id = :userId AND done = 1")
                .param("userId", userId)
                .update();
    }

    private static String lower(String text) {
        return text.toLowerCase(Locale.ROOT);
    }
}
