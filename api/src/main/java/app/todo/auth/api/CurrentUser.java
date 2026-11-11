package app.todo.auth.api;

/**
 * The only type the identity module publishes to other modules (sdd.md section 2, ADR-0018): the Spring Security
 * principal of every request that carries a valid session. {@code userId} is an opaque number to the to-dos module,
 * which never reads a cookie, a session or a user row.
 */
public record CurrentUser(long userId) {}
