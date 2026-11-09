package app.todo.todo;

/**
 * A validated list request (REQ-LIST-001..003). {@code search} is trimmed and lower-cased with {@code Locale.ROOT},
 * empty for no search; {@code page} is the requested 1-based page, which the repository clamps to the last page.
 */
record TodoQuery(Status status, String search, long page) {

    static final int PAGE_SIZE = 20;

    enum Status {
        ALL,
        ACTIVE,
        COMPLETED
    }
}
