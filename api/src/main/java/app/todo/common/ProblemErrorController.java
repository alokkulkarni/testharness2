package app.todo.common;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Replaces Spring Boot's {@code /error} handling (sdd.md section 6, ADR-0011): anything the servlet container forwards
 * to {@code /error} is answered as a Problem through {@link ProblemWriter}. The whitelabel page and Boot's default
 * body are never produced. The forwarded exception, message and URI are not used, so no exception text can reach the
 * client, and this controller logs no stack trace (the request id filter or the advice already did).
 */
@Controller
public class ProblemErrorController implements ErrorController {

    private final ProblemWriter writer;

    ProblemErrorController(ProblemWriter writer) {
        this.writer = writer;
    }

    @RequestMapping("/error")
    void error(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Object attribute = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int forwarded = attribute instanceof Integer status ? status : 404;
        switch (forwarded) {
            case 401 -> writer.write(response, 401, ProblemCode.UNAUTHENTICATED);
            case 404 -> writer.write(response, 404, ProblemCode.NOT_FOUND);
            case 405 -> writer.write(response, 405, ProblemCode.METHOD_NOT_ALLOWED);
            case 415 -> writer.write(response, 415, ProblemCode.UNSUPPORTED_MEDIA_TYPE);
            default -> {
                if (forwarded >= 400 && forwarded < 500) {
                    writer.write(response, forwarded, ProblemCode.REQUEST_REJECTED);
                } else {
                    writer.write(response, forwarded >= 500 && forwarded < 600 ? forwarded : 500, ProblemCode.INTERNAL_ERROR);
                }
            }
        }
    }
}
