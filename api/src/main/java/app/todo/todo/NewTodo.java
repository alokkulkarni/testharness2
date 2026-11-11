package app.todo.todo;

import java.time.Instant;

/** A validated to-do ready to be stored: its content and the creation instant (ADR-0013). */
record NewTodo(TodoContent content, Instant createdAt) {}
