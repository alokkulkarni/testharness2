package app.todo.common;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Maps application and Spring MVC exceptions to Problem responses through {@link ProblemWriter} (sdd.md section 6,
 * ADR-0011). It maps any {@link ApiException} generically and names no module's exception type. Unexpected exceptions
 * become the generic 500 and their stack trace is logged once, with the request id from the logging context.
 */
@RestControllerAdvice
public class ProblemDetailsAdvice {

    private static final Logger LOG = LoggerFactory.getLogger(ProblemDetailsAdvice.class);

    private final ProblemWriter writer;

    ProblemDetailsAdvice(ProblemWriter writer) {
        this.writer = writer;
    }

    @ExceptionHandler(ApiException.class)
    void apiException(ApiException e, HttpServletResponse response) throws IOException {
        writer.write(response, e.status(), e.code(), e.detail(), e.errors());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    void unreadableBody(HttpServletResponse response) throws IOException {
        writer.write(response, 400, ProblemCode.MALFORMED_REQUEST);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    void unsupportedMediaType(HttpServletResponse response) throws IOException {
        writer.write(response, 415, ProblemCode.UNSUPPORTED_MEDIA_TYPE);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    void methodNotAllowed(HttpRequestMethodNotSupportedException e, HttpServletResponse response) throws IOException {
        Set<HttpMethod> supported = e.getSupportedHttpMethods();
        if (supported != null && !supported.isEmpty()) {
            response.setHeader(HttpHeaders.ALLOW,
                    supported.stream().map(HttpMethod::name).sorted().collect(Collectors.joining(", ")));
        }
        writer.write(response, 405, ProblemCode.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    void unknownRoute(HttpServletResponse response) throws IOException {
        writer.write(response, 404, ProblemCode.NOT_FOUND);
    }

    @ExceptionHandler(TypeMismatchException.class)
    void typeMismatch(HttpServletResponse response) throws IOException {
        writer.write(response, 400, ProblemCode.REQUEST_REJECTED);
    }

    /** Any other exception: a framework 4xx is a refused request; everything else is the generic 500. */
    @ExceptionHandler(Exception.class)
    void anyOther(Exception e, HttpServletResponse response) throws IOException {
        if (e instanceof ErrorResponse errorResponse && errorResponse.getStatusCode().is4xxClientError()) {
            writer.write(response, errorResponse.getStatusCode().value(), ProblemCode.REQUEST_REJECTED);
            return;
        }
        LOG.error("Unexpected error while handling a request", e);
        writer.write(response, 500, ProblemCode.INTERNAL_ERROR, ProblemCode.INTERNAL_ERROR.detail(), List.of());
    }
}
