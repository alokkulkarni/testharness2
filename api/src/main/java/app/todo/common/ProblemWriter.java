package app.todo.common;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * The only code that builds and writes an error body (sdd.md section 6, ADR-0011): an RFC 9457
 * {@code application/problem+json} document shaped as {@code Problem} in {@code common-problem-v1.yaml}. It writes
 * directly to the response, independent of the request's {@code Accept} header.
 */
@Component
public class ProblemWriter {

    public static final String MEDIA_TYPE = "application/problem+json";

    public void write(HttpServletResponse response, int status, ProblemCode code, String detail, List<FieldError> errors)
            throws IOException {
        StringBuilder json = new StringBuilder(256);
        json.append("{\"type\":").append(quote(code.type()));
        json.append(",\"title\":").append(quote(code.title()));
        json.append(",\"status\":").append(status);
        if (detail != null) {
            json.append(",\"detail\":").append(quote(detail));
        }
        json.append(",\"code\":").append(quote(code.name()));
        json.append(",\"errors\":[");
        for (int i = 0; i < errors.size(); i++) {
            FieldError error = errors.get(i);
            if (i > 0) {
                json.append(',');
            }
            json.append("{\"field\":").append(quote(error.field()));
            json.append(",\"code\":").append(quote(error.code().name()));
            json.append(",\"message\":").append(quote(error.message())).append('}');
        }
        json.append("]}");

        byte[] body = json.toString().getBytes(StandardCharsets.UTF_8);
        if (!response.isCommitted()) {
            response.resetBuffer();
        }
        response.setStatus(status);
        response.setContentType(MEDIA_TYPE);
        response.setContentLength(body.length);
        response.getOutputStream().write(body);
        response.flushBuffer();
    }

    /** Writes the problem with the code's own fixed detail sentence and no field errors. */
    public void write(HttpServletResponse response, int status, ProblemCode code) throws IOException {
        write(response, status, code, code.detail(), List.of());
    }

    private static String quote(String value) {
        StringBuilder sb = new StringBuilder(value.length() + 2).append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }
}
