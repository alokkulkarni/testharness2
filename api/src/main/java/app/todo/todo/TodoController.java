package app.todo.todo;

import app.todo.auth.api.CurrentUser;
import java.net.URI;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The to-dos module's operations ({@code todo-api-v1.yaml}): {@code listTodos}, {@code createTodo}, {@code getTodo},
 * {@code updateTodo}, {@code setTodoDone}, {@code deleteTodo}, {@code clearCompletedTodos}. The owner comes only from
 * the {@link CurrentUser} principal, never from the request (REQ-ISO-001).
 */
@RestController
@RequestMapping("/api/v1/todos")
class TodoController {

    private final TodoService todos;

    TodoController(TodoService todos) {
        this.todos = todos;
    }

    @GetMapping
    TodoPage list(@AuthenticationPrincipal CurrentUser user,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "page", required = false) String page) {
        return todos.list(user.userId(), status, q, page);
    }

    @PostMapping(consumes = "application/json")
    ResponseEntity<Todo> create(@AuthenticationPrincipal CurrentUser user, @RequestBody Map<String, Object> request) {
        Todo created = todos.create(user.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).location(URI.create("/api/v1/todos/" + created.id())).body(created);
    }

    @DeleteMapping("/completed")
    ClearCompletedResult clearCompleted(@AuthenticationPrincipal CurrentUser user) {
        return new ClearCompletedResult(todos.clearCompleted(user.userId()));
    }

    @GetMapping("/{todoId}")
    Todo get(@AuthenticationPrincipal CurrentUser user, @PathVariable String todoId) {
        return todos.get(user.userId(), todoId);
    }

    @PutMapping(value = "/{todoId}", consumes = "application/json")
    Todo update(@AuthenticationPrincipal CurrentUser user, @PathVariable String todoId,
            @RequestBody Map<String, Object> request) {
        return todos.update(user.userId(), todoId, request);
    }

    @DeleteMapping("/{todoId}")
    ResponseEntity<Void> delete(@AuthenticationPrincipal CurrentUser user, @PathVariable String todoId) {
        todos.delete(user.userId(), todoId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/{todoId}/done", consumes = "application/json")
    Todo setDone(@AuthenticationPrincipal CurrentUser user, @PathVariable String todoId,
            @RequestBody Map<String, Object> request) {
        return todos.setDone(user.userId(), todoId, request);
    }

    public record ClearCompletedResult(int deletedCount) {}
}
