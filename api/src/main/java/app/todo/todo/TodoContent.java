package app.todo.todo;

/**
 * The validated content fields of a to-do: {@code title} trimmed, {@code description} as sent or null when absent or
 * blank, {@code dueDate} as {@code YYYY-MM-DD} text.
 */
record TodoContent(String title, String description, String dueDate, Priority priority) {}
