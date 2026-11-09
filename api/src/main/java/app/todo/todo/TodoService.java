package app.todo.todo;

import app.todo.common.NotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * To-do operations for one owner (sdd.md section 7). The owner is always the {@code userId} taken from the session by
 * the controller. Last write wins: no version check (REQ-DATA-002). Validation runs before ownership, so an invalid
 * body is a 400 even for an id that does not exist.
 */
@Service
class TodoService {

    private final TodoRepository todos;
    private final TodoValidator validator;
    private final Clock clock;
    private final TransactionTemplate transaction;

    TodoService(TodoRepository todos, TodoValidator validator, Clock clock, TransactionTemplate transaction) {
        this.todos = todos;
        this.validator = validator;
        this.clock = clock;
        this.transaction = transaction;
    }

    TodoPage list(long userId, String status, String q, String page) {
        TodoQuery query = validator.query(status, q, page);
        // the page and the totals are read in one transaction so they agree
        return Objects.requireNonNull(transaction.execute(unused -> {
            TodoPageResult result = todos.list(userId, query);
            TodoTotals totals = todos.totals(userId);
            return new TodoPage(result.items(), result.page(), TodoQuery.PAGE_SIZE, result.totalItems(), result.totalPages(), totals);
        }));
    }

    Todo get(long userId, String id) {
        return todos.findByPublicId(userId, id).orElseThrow(NotFoundException::new);
    }

    Todo create(long userId, Map<String, Object> request) {
        TodoContent content = validator.forCreate(request);
        return todos.insert(userId, new NewTodo(content, now()));
    }

    Todo update(long userId, String id, Map<String, Object> request) {
        TodoContent content = validator.forUpdate(request);
        Instant now = now();
        return Objects.requireNonNull(transaction.execute(unused -> {
            if (!todos.updateContent(userId, id, content, now)) {
                throw new NotFoundException();
            }
            return get(userId, id);
        }));
    }

    Todo setDone(long userId, String id, Map<String, Object> request) {
        boolean done = validator.done(request);
        Instant now = now();
        return Objects.requireNonNull(transaction.execute(unused -> {
            if (!todos.updateDone(userId, id, done, now)) {
                throw new NotFoundException();
            }
            return get(userId, id);
        }));
    }

    void delete(long userId, String id) {
        if (!todos.delete(userId, id)) {
            throw new NotFoundException();
        }
    }

    int clearCompleted(long userId) {
        return todos.deleteCompleted(userId);
    }

    private Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.MILLIS);
    }
}
