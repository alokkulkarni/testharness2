package app.todo.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.security.web.firewall.RequestRejectedHandler;
import org.springframework.stereotype.Component;

/** Spring Security firewall rejections (for example {@code ;} or an encoded {@code /} in the path): 400 {@code REQUEST_REJECTED}. */
@Component
public class ProblemRequestRejectedHandler implements RequestRejectedHandler {

    private final ProblemWriter writer;

    ProblemRequestRejectedHandler(ProblemWriter writer) {
        this.writer = writer;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, RequestRejectedException rejected)
            throws IOException {
        writer.write(response, 400, ProblemCode.REQUEST_REJECTED);
    }
}
