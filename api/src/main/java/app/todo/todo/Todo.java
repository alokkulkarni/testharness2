package app.todo.todo;

/**
 * A to-do as the API returns it ({@code Todo} in {@code todo-api-v1.yaml}). {@code id} is the random public id, never
 * the internal row id; {@code description} is null when there is none; the timestamps are UTC text with milliseconds.
 */
public record Todo(
        String id,
        String title,
        String description,
        String dueDate,
        Priority priority,
        boolean done,
        String createdAt,
        String updatedAt) {}
