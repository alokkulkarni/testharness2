package app.todo.todo;

import java.time.Instant;
import java.util.Optional;

/**
 * Package-private store of the {@code todos} table (ADR-0009). Every method takes the owner as a required parameter and
 * every statement filters on it in the same statement as the id, so a missing id, a malformed id and another user's id
 * are indistinguishable and there is no read-then-write gap (sdd.md section 7). The SQL names only {@code todos}.
 */
interface TodoRepository {

    Optional<Todo> findByPublicId(long userId, String publicId);

    /** The page for the query; a page past the last is clamped to the last, or page 1 when nothing matches. */
    TodoPageResult list(long userId, TodoQuery query);

    TodoTotals totals(long userId);

    Todo insert(long userId, NewTodo todo);

    /** Replaces the content fields; {@code updated_at} becomes max(updatedAt, previous + 1 ms). False when no row matched. */
    boolean updateContent(long userId, String publicId, TodoContent content, Instant updatedAt);

    /** Sets {@code done}; {@code updated_at} becomes max(updatedAt, previous + 1 ms). False when no row matched. */
    boolean updateDone(long userId, String publicId, boolean done, Instant updatedAt);

    boolean delete(long userId, String publicId);

    /** Deletes every completed to-do of the owner and returns how many. */
    int deleteCompleted(long userId);
}
