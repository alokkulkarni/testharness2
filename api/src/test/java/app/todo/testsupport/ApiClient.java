package app.todo.testsupport;

import static org.assertj.core.api.Assertions.assertThat;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Black-box HTTP client for the running API. Every exchange is checked against the frozen contract
 * (ADR-0012) unless the caller picks a lighter {@link Check}.
 */
public final class ApiClient {

    /** How much of an exchange is validated against the module contract file. */
    public enum Check {
        /** Request and response (use for requests the contract says are valid). */
        FULL,
        /** Response only (use when the request is deliberately invalid or lacks the client header). */
        RESPONSE_ONLY,
        /** No contract check (405, unknown routes and other answers with no contract operation). */
        NONE
    }

    private static final Set<String> UNSAFE = Set.of("POST", "PUT", "DELETE", "PATCH");
    private static final String JSON = "application/json";
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final String base;
    private final String sessionToken;
    private final boolean clientHeader;

    private ApiClient(String base, String sessionToken, boolean clientHeader) {
        this.base = base;
        this.sessionToken = sessionToken;
        this.clientHeader = clientHeader;
    }

    public static ApiClient forPort(int port) {
        return new ApiClient("http://localhost:" + port, null, true);
    }

    public ApiClient withSession(String token) {
        return new ApiClient(base, token, clientHeader);
    }

    public ApiClient withoutClientHeader() {
        return new ApiClient(base, sessionToken, false);
    }

    public String sessionToken() {
        return sessionToken;
    }

    public String baseUrl() {
        return base;
    }

    // ---- convenience: valid requests, request and response checked ----

    public Reply get(String pathAndQuery) {
        return send("GET", pathAndQuery, null, JSON, Check.FULL);
    }

    public Reply post(String path, String body) {
        return send("POST", path, body, JSON, Check.FULL);
    }

    public Reply put(String path, String body) {
        return send("PUT", path, body, JSON, Check.FULL);
    }

    public Reply delete(String path) {
        return send("DELETE", path, null, JSON, Check.FULL);
    }

    // ---- convenience: deliberately invalid requests, response checked ----

    public Reply getInvalid(String pathAndQuery) {
        return send("GET", pathAndQuery, null, JSON, Check.RESPONSE_ONLY);
    }

    public Reply postInvalid(String path, String body) {
        return send("POST", path, body, JSON, Check.RESPONSE_ONLY);
    }

    public Reply putInvalid(String path, String body) {
        return send("PUT", path, body, JSON, Check.RESPONSE_ONLY);
    }

    public Reply deleteInvalid(String path) {
        return send("DELETE", path, null, JSON, Check.RESPONSE_ONLY);
    }

    /** No contract check at all. */
    public Reply raw(String method, String pathAndQuery, String body, String contentType) {
        return send(method, pathAndQuery, body, contentType, Check.NONE);
    }

    public Reply send(String method, String pathAndQuery, String body, String contentType, Check check) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(base + pathAndQuery))
                .timeout(Duration.ofSeconds(30));
        if (contentType != null) {
            builder.header("Content-Type", contentType);
        }
        if (clientHeader && UNSAFE.contains(method)) {
            builder.header("X-Todo-Client", "web");
        }
        if (sessionToken != null) {
            builder.header("Cookie", "todo_session=" + sessionToken);
        }
        HttpRequest.BodyPublisher publisher =
                body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body);
        builder.method(method, publisher);

        HttpResponse<String> response;
        try {
            response = HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("request failed: " + method + " " + pathAndQuery, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        Reply reply = new Reply(response.statusCode(), response.body(), response.headers().map());
        if (check != Check.NONE) {
            validate(method, pathAndQuery, body, contentType, reply, check);
        }
        return reply;
    }

    private void validate(String method, String pathAndQuery, String body, String contentType, Reply reply, Check check) {
        String path = pathAndQuery;
        Map<String, String> query = new LinkedHashMap<>();
        int q = pathAndQuery.indexOf('?');
        if (q >= 0) {
            path = pathAndQuery.substring(0, q);
            for (String pair : pathAndQuery.substring(q + 1).split("&")) {
                if (pair.isEmpty()) {
                    continue;
                }
                int eq = pair.indexOf('=');
                String name = URLDecoder.decode(eq < 0 ? pair : pair.substring(0, eq), StandardCharsets.UTF_8);
                String value = eq < 0 ? "" : URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
                query.put(name, value);
            }
        }
        OpenApiInteractionValidator validator = Contracts.validatorFor(path);
        Request.Method verb = Request.Method.valueOf(method);

        SimpleResponse.Builder rb = SimpleResponse.Builder.status(reply.status());
        if (reply.body() != null && !reply.body().isEmpty()) {
            rb.withBody(reply.body());
        }
        if (reply.contentType() != null) {
            rb.withContentType(reply.contentType());
        }
        reply.headers().forEach((name, values) -> {
            if (!name.equalsIgnoreCase("content-type") && !name.startsWith(":")) {
                rb.withHeader(name, values.toArray(new String[0]));
            }
        });

        ValidationReport report;
        if (check == Check.FULL) {
            SimpleRequest.Builder qb = new SimpleRequest.Builder(verb, path);
            if (body != null) {
                qb.withBody(body);
            }
            if (contentType != null && body != null) {
                qb.withContentType(contentType);
            }
            if (clientHeader && UNSAFE.contains(method)) {
                qb.withHeader("X-Todo-Client", "web");
            }
            query.forEach(qb::withQueryParam);
            report = validator.validate(qb.build(), rb.build());
        } else {
            report = validator.validateResponse(path, verb, rb.build());
        }
        List<String> problems = report.getMessages().stream()
                .map(m -> m.getKey() + ": " + m.getMessage())
                .collect(Collectors.toList());
        assertThat(report.hasErrors())
                .as("contract violations for %s %s (status %d, body %s): %s", method, pathAndQuery, reply.status(), reply.body(), problems)
                .isFalse();
    }
}
