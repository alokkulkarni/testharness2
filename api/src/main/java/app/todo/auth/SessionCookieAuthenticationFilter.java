package app.todo.auth;

import app.todo.auth.api.CurrentUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Resolves the {@code todo_session} cookie to a user on each request (sdd.md section 4, ADR-0004). A missing, unknown
 * or expired session leaves the request unauthenticated; a valid one makes it authenticated with a {@link CurrentUser}
 * principal. It does not catch unexpected exceptions (they reach the request id filter) and runs on the REQUEST
 * dispatch only. It only puts the internal user id into the logging context, under a plain key.
 *
 * <p>Created by the security configuration; it is not a servlet-registered bean.
 */
public final class SessionCookieAuthenticationFilter extends OncePerRequestFilter {

    public static final String COOKIE_NAME = "todo_session";
    private static final String USER_ID_KEY = "userId";

    private final SessionService sessions;

    public SessionCookieAuthenticationFilter(SessionService sessions) {
        this.sessions = sessions;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = tokenOf(request);
        if (token != null) {
            Optional<SessionService.ActiveSession> session = sessions.resolve(token);
            if (session.isPresent()) {
                UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                        new CurrentUser(session.get().userId()), null, AuthorityUtils.NO_AUTHORITIES);
                authentication.setDetails(new SessionDetails(session.get().expiresAt()));
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);
                MDC.put(USER_ID_KEY, Long.toString(session.get().userId()));
            }
        }
        chain.doFilter(request, response);
    }

    /** The cookie's token, or null when the cookie is absent or empty. */
    static String tokenOf(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isEmpty()) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
