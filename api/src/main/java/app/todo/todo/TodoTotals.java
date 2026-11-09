package app.todo.todo;

/** Counts of all of the caller's to-dos, ignoring status, search and page ({@code TodoTotals}). */
public record TodoTotals(long all, long active, long completed) {}
