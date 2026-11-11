package app.todo.common;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Request id and the one per-request log line (sdd.md section 10). A servlet filter registered ahead of the Spring
 * Security chain for the REQUEST and ERROR dispatches (by the composition root), so it runs before and around every
 * other filter.
 *
 * <ul>
 *   <li>REQUEST dispatch: generates the id, puts it in the logging context under {@value #REQUEST_ID_KEY} and in the
 *       request attribute {@value #REQUEST_ID_ATTRIBUTE}, runs the chain, and in {@code finally} writes the request's
 *       log line (status 500 and, as a separate entry, the stack trace when an exception escapes the chain), then
 *       clears the whole logging context and rethrows unchanged.
 *   <li>ERROR dispatch: restores the id from the attribute so what {@code ProblemErrorController} logs carries it,
 *       runs the chain, clears the context, and writes no second line. Only if the attribute is absent is it treated
 *       as a REQUEST dispatch.
 * </ul>
 *
 * Request bodies, identifiers, passwords, cookies, titles and search text are never logged here, only the method, the
 * route template, the status and the duration.
 */
public final class RequestIdFilter implements Filter {

    public static final String REQUEST_ID_KEY = "requestId";
    public static final String REQUEST_ID_ATTRIBUTE = "app.todo.requestId";

    private static final Logger LOG = LoggerFactory.getLogger(RequestIdFilter.class);

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        if (request.getDispatcherType() == DispatcherType.ERROR && request.getAttribute(REQUEST_ID_ATTRIBUTE) instanceof String id) {
            MDC.put(REQUEST_ID_KEY, id);
            try {
                chain.doFilter(request, response);
            } finally {
                MDC.clear();
            }
            return;
        }

        request.setAttribute(REQUEST_ID_ATTRIBUTE, newRequestId());
        MDC.put(REQUEST_ID_KEY, (String) request.getAttribute(REQUEST_ID_ATTRIBUTE));
        long started = System.nanoTime();
        Throwable failure = null;
        try {
            chain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException | Error e) {
            failure = e;
            throw e;
        } finally {
            logRequest(request, response, failure, (System.nanoTime() - started) / 1_000_000);
            MDC.clear();
        }
    }

    private static void logRequest(HttpServletRequest request, HttpServletResponse response, Throwable failure, long millis) {
        // an exception propagating out of the chain is answered 500 by the container
        int status = failure == null ? response.getStatus() : 500;
        Object route = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (route != null) {
            LOG.info("request method={} route={} status={} durationMs={}", request.getMethod(), route, status, millis);
        } else {
            LOG.info("request method={} status={} durationMs={}", request.getMethod(), status, millis);
        }
        if (failure != null) {
            LOG.error("Unhandled exception while processing a request", failure);
        }
    }

    /** An opaque id; one without the digit run "500" so a grep for a status code never matches an id. */
    private static String newRequestId() {
        String id;
        do {
            id = UUID.randomUUID().toString().replace("-", "");
        } while (id.contains("500"));
        return id;
    }
}
