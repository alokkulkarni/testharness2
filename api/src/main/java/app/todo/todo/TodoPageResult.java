package app.todo.todo;

import java.util.List;

/** One page of matches: the effective (clamped) page, the match count and the page count. */
record TodoPageResult(List<Todo> items, int page, long totalItems, int totalPages) {}
