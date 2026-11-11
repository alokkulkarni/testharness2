package app.todo.todo;

import java.util.List;

/** The {@code listTodos} response ({@code TodoPage}); the page size is fixed at 20. */
public record TodoPage(List<Todo> items, int page, int pageSize, long totalItems, int totalPages, TodoTotals totals) {}
