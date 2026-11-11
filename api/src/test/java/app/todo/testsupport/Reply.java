package app.todo.testsupport;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** One HTTP response: status, body text and headers (names lower-cased). */
public record Reply(int status, String body, Map<String, List<String>> headers) {

    public String header(String name) {
        List<String> values = headers.get(name.toLowerCase(Locale.ROOT));
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    public List<String> headerValues(String name) {
        List<String> values = headers.get(name.toLowerCase(Locale.ROOT));
        return values == null ? List.of() : values;
    }

    public String contentType() {
        return header("content-type");
    }

    public <T> T json(String path) {
        return JsonPath.read(body, path);
    }

    public boolean has(String path) {
        try {
            Object v = JsonPath.read(body, path);
            return v != null;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** The whole {@code Set-Cookie} header line that sets {@code todo_session}, or null. */
    public String sessionCookieLine() {
        for (String line : headerValues("set-cookie")) {
            if (line.startsWith("todo_session=")) {
                return line;
            }
        }
        return null;
    }

    /** The session token from the {@code todo_session} cookie, or null when the response sets none. */
    public String sessionToken() {
        String line = sessionCookieLine();
        if (line == null) {
            return null;
        }
        int end = line.indexOf(';');
        return line.substring("todo_session=".length(), end < 0 ? line.length() : end);
    }

    /** Asserts an RFC 9457 error body with the given status and contract {@code code}, valid against {@code Problem}. */
    public void assertProblem(int expectedStatus, String expectedCode) {
        org.assertj.core.api.Assertions.assertThat(status).as("status of %s", body).isEqualTo(expectedStatus);
        org.assertj.core.api.Assertions.assertThat(contentType()).as("content type").startsWith("application/problem+json");
        org.assertj.core.api.Assertions.assertThat(this.<String>json("$.code")).as("code of %s", body).isEqualTo(expectedCode);
        org.assertj.core.api.Assertions.assertThat(this.<Integer>json("$.status")).isEqualTo(expectedStatus);
        org.assertj.core.api.Assertions.assertThat(this.<String>json("$.type"))
                .isEqualTo("urn:todo:problem:" + expectedCode.toLowerCase(Locale.ROOT).replace('_', '-'));
        Contracts.assertValidProblem(body);
    }

    /** The {@code errors[].field} values of a Problem body. */
    public List<String> errorFields() {
        return json("$.errors[*].field");
    }

    /** The {@code errors[].code} of the entry for a field, or null. */
    public String errorCodeFor(String field) {
        List<String> codes = json("$.errors[?(@.field=='" + field + "')].code");
        return codes.isEmpty() ? null : codes.get(0);
    }
}
