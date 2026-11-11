package app.todo.testsupport;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static org.assertj.core.api.Assertions.assertThat;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.mockmvc.MockMvcResponse;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;
import jakarta.servlet.http.Cookie;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Client for the API under test. Two transports (sdd.md section 14):
 * <ul>
 *   <li>{@link #forMockMvc(MockMvc)}: Spring Boot + MockMvc, the integration level of every test. Each exchange is
 *       validated against the module contract file with the openapi-request-validator v3 MockMvc adapter
 *       ({@code OpenApiValidationMatchers.openApi().isValid(validator)}) unless the caller picks a lighter
 *       {@link Check}.</li>
 *   <li>{@link #forPort(int)}: the embedded container at a random port, used only by the NFR-004 filter-exception
 *       test (MockMvc performs no ERROR dispatch). Its exchange is validated with the core validator's
 *       {@code OpenApiInteractionValidator} instead of the MockMvc adapter.</li>
 * </ul>
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

    private final MockMvc mockMvc;
    private final String base;
    private final String sessionToken;
    /** Value of the X-Todo-Client header sent on unsafe methods, or null to send none. */
    private final String clientHeader;

    private ApiClient(MockMvc mockMvc, String base, String sessionToken, String clientHeader) {
        this.mockMvc = mockMvc;
        this.base = base;
        this.sessionToken = sessionToken;
        this.clientHeader = clientHeader;
    }

    public static ApiClient forMockMvc(MockMvc mockMvc) {
        return new ApiClient(mockMvc, null, null, "web");
    }

    /** The embedded container at {@code port}: only for the filter-exception test. */
    public static ApiClient forPort(int port) {
        return new ApiClient(null, "http://localhost:" + port, null, "web");
    }

    public ApiClient withSession(String token) {
        return new ApiClient(mockMvc, base, token, clientHeader);
    }

    public ApiClient withoutClientHeader() {
        return new ApiClient(mockMvc, base, sessionToken, null);
    }

    /** Sends {@code value} as the X-Todo-Client header on unsafe methods (for a wrong value). */
    public ApiClient withClientHeader(String value) {
        return new ApiClient(mockMvc, base, sessionToken, value);
    }

    public String sessionToken() {
        return sessionToken;
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
        return mockMvc != null
                ? sendMockMvc(method, pathAndQuery, body, contentType, check)
                : sendHttp(method, pathAndQuery, body, contentType, check);
    }

    // ---- MockMvc transport ----

    private Reply sendMockMvc(String method, String pathAndQuery, String body, String contentType, Check check) {
        MockHttpServletRequestBuilder builder =
                MockMvcRequestBuilders.request(HttpMethod.valueOf(method), URI.create(pathAndQuery));
        if (contentType != null) {
            builder.header("Content-Type", contentType);
        }
        if (clientHeader != null && UNSAFE.contains(method)) {
            builder.header("X-Todo-Client", clientHeader);
        }
        if (sessionToken != null) {
            builder.cookie(new Cookie("todo_session", sessionToken));
        }
        if (body != null) {
            builder.content(body.getBytes(StandardCharsets.UTF_8));
        }
        MvcResult result;
        try {
            result = mockMvc.perform(builder).andReturn();
        } catch (Exception e) {
            throw new IllegalStateException("request failed: " + method + " " + pathAndQuery, e);
        }
        MockHttpServletResponse response = result.getResponse();
        Map<String, List<String>> headers = new LinkedHashMap<>();
        for (String name : response.getHeaderNames()) {
            headers.put(name.toLowerCase(Locale.ROOT), new ArrayList<>(response.getHeaders(name)));
        }
        if (response.getContentType() != null) {
            headers.put("content-type", List.of(response.getContentType()));
        }
        Reply reply;
        try {
            reply = new Reply(response.getStatus(), response.getContentAsString(StandardCharsets.UTF_8), headers);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        if (check == Check.FULL) {
            String path = pathOf(pathAndQuery);
            try {
                openApi().isValid(Contracts.validatorFor(path)).match(result);
            } catch (Exception | AssertionError e) {
                throw new AssertionError("contract violations for " + method + " " + pathAndQuery + " (status "
                        + reply.status() + ", body " + reply.body() + "): " + e.getMessage(), e);
            }
        } else if (check == Check.RESPONSE_ONLY) {
            String path = pathOf(pathAndQuery);
            ValidationReport report = Contracts.validatorFor(path)
                    .validateResponse(path, Request.Method.valueOf(method), MockMvcResponse.of(response));
            assertNoErrors(report, method, pathAndQuery, reply);
        }
        return reply;
    }

    // ---- embedded-container transport (filter-exception test only) ----

    private Reply sendHttp(String method, String pathAndQuery, String body, String contentType, Check check) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(base + pathAndQuery))
                .timeout(Duration.ofSeconds(30));
        if (contentType != null) {
            builder.header("Content-Type", contentType);
        }
        if (clientHeader != null && UNSAFE.contains(method)) {
            builder.header("X-Todo-Client", clientHeader);
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
            validateWithCoreValidator(method, pathAndQuery, body, contentType, reply, check);
        }
        return reply;
    }

    private static String pathOf(String pathAndQuery) {
        int q = pathAndQuery.indexOf('?');
        return q >= 0 ? pathAndQuery.substring(0, q) : pathAndQuery;
    }

    private void validateWithCoreValidator(String method, String pathAndQuery, String body, String contentType, Reply reply, Check check) {
        String path = pathOf(pathAndQuery);
        Map<String, String> query = new LinkedHashMap<>();
        int q = pathAndQuery.indexOf('?');
        if (q >= 0) {
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
            if (clientHeader != null && UNSAFE.contains(method)) {
                qb.withHeader("X-Todo-Client", clientHeader);
            }
            query.forEach(qb::withQueryParam);
            report = validator.validate(qb.build(), rb.build());
        } else {
            report = validator.validateResponse(path, verb, rb.build());
        }
        assertNoErrors(report, method, pathAndQuery, reply);
    }

    private static void assertNoErrors(ValidationReport report, String method, String pathAndQuery, Reply reply) {
        List<String> problems = report.getMessages().stream()
                .map(m -> m.getKey() + ": " + m.getMessage())
                .collect(Collectors.toList());
        assertThat(report.hasErrors())
                .as("contract violations for %s %s (status %d, body %s): %s", method, pathAndQuery, reply.status(), reply.body(), problems)
                .isFalse();
    }
}
