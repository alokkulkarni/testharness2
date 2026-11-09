package app.todo.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Cross-site request defence (ADR-0007): every unsafe request under {@code /api/} must carry {@code X-Todo-Client: web},
 * otherwise 403 {@code CLIENT_HEADER_MISSING}. It guards both modules' unsafe routes, so it lives in the shared kernel.
 * It is created by the security configuration and placed after the authorisation check, so a request without a session
 * is answered 401 before this 403 (sdd.md section 4, "Order of checks"). It is not a servlet-registered bean.
 */
public final class ClientHeaderFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Todo-Client";
    public static final String VALUE = "web";

    private static final Set<String> UNSAFE = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final ProblemWriter writer;

    public ClientHeaderFilter(ProblemWriter writer) {
        this.writer = writer;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !UNSAFE.contains(request.getMethod()) || !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!VALUE.equals(request.getHeader(HEADER))) {
            writer.write(response, 403, ProblemCode.CLIENT_HEADER_MISSING);
            return;
        }
        chain.doFilter(request, response);
    }
}
