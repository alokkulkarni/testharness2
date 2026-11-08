package app.todo.testsupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Checks the contract tooling the other tests rely on (ADR-0012): the frozen files load with their $refs, accept
 * conforming bodies and reject non-conforming ones. These need no running API.
 */
class ContractFilesTest {

    private static final String TODO = """
            {"id":"6f1c2b8e-3a4d-4c5e-9f60-1a2b3c4d5e6f","title":"t","description":null,"dueDate":"2026-12-01",
             "priority":"LOW","done":false,"createdAt":"2026-11-08T10:00:00.000Z","updatedAt":"2026-11-08T10:00:00.000Z"}""";

    private static SimpleResponse json(int status, String body) {
        return SimpleResponse.Builder.status(status).withContentType("application/json").withBody(body).build();
    }

    private static SimpleResponse problem(int status, String body) {
        return SimpleResponse.Builder.status(status).withContentType("application/problem+json").withBody(body).build();
    }

    @Test
    @DisplayName("NFR-004 AC-2 the Problem schema accepts a conforming error body and rejects an unknown code")
    void NFR_004_AC2_problem_schema_accepts_and_rejects() {
        Contracts.assertValidProblem(
                "{\"type\":\"urn:todo:problem:not-found\",\"title\":\"Not found\",\"status\":404,\"code\":\"NOT_FOUND\",\"errors\":[]}");

        assertThatThrownBy(() -> Contracts.assertValidProblem(
                "{\"type\":\"urn:todo:problem:bogus\",\"title\":\"x\",\"status\":404,\"code\":\"BOGUS\",\"errors\":[]}"))
                .isInstanceOf(AssertionError.class);
        assertThatThrownBy(() -> Contracts.assertValidProblem("{\"title\":\"x\",\"status\":404}"))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    @DisplayName("NFR-004 AC-2 the to-dos module contract accepts a conforming listTodos response and rejects an unknown member")
    void NFR_004_AC2_todos_contract_validates_responses() {
        String page = "{\"items\":[" + TODO + "],\"page\":1,\"pageSize\":20,\"totalItems\":1,\"totalPages\":1,"
                + "\"totals\":{\"all\":1,\"active\":1,\"completed\":0}}";
        var validator = Contracts.validatorFor("/api/v1/todos");

        assertThat(validator.validateResponse("/api/v1/todos", Request.Method.GET, json(200, page)).hasErrors()).isFalse();
        assertThat(validator.validateResponse("/api/v1/todos", Request.Method.GET,
                json(200, page.replace("\"pageSize\":20", "\"pageSize\":20,\"extra\":1"))).hasErrors()).isTrue();
        assertThat(validator.validateResponse("/api/v1/todos", Request.Method.GET,
                json(200, page.replace("\"pageSize\":20", "\"pageSize\":5"))).hasErrors()).isTrue();
    }

    @Test
    @DisplayName("NFR-004 AC-2 the identity module contract resolves the shared Problem response through its $ref")
    void NFR_004_AC2_identity_contract_resolves_the_common_file() {
        var validator = Contracts.validatorFor("/api/v1/auth/login");
        String body = "{\"type\":\"urn:todo:problem:invalid-credentials\",\"title\":\"Invalid credentials\",\"status\":401,"
                + "\"code\":\"INVALID_CREDENTIALS\",\"detail\":\"Invalid identifier or password.\",\"errors\":[]}";

        assertThat(validator.validateResponse("/api/v1/auth/login", Request.Method.POST, problem(401, body)).hasErrors()).isFalse();
        assertThat(validator.validateResponse("/api/v1/auth/login", Request.Method.POST, problem(418, body)).hasErrors()).isTrue();
    }

    @Test
    @DisplayName("REQ-TODO-002 AC-5 the contract request check rejects a create without a due date and a missing client header")
    void REQ_TODO_002_AC5_request_validation_rejects_missing_due_date_and_missing_header() {
        var validator = Contracts.validatorFor("/api/v1/todos");
        String bad = "{\"title\":\"t\",\"priority\":\"HIGH\"}";
        String good = "{\"title\":\"t\",\"dueDate\":\"2026-12-01\",\"priority\":\"HIGH\"}";

        var goodReport = validator.validateRequest(createRequest(good, true));
        var badReport = validator.validateRequest(createRequest(bad, true));
        var noHeaderReport = validator.validateRequest(createRequest(good, false));

        assertThat(goodReport.hasErrors()).as("conforming request: %s", goodReport.getMessages()).isFalse();
        assertThat(badReport.hasErrors()).as("no dueDate: %s", badReport.getMessages()).isTrue();
        assertThat(noHeaderReport.hasErrors()).as("no X-Todo-Client: %s", noHeaderReport.getMessages()).isTrue();
    }

    private static SimpleRequest createRequest(String body, boolean clientHeader) {
        SimpleRequest.Builder builder = new SimpleRequest.Builder(Request.Method.POST, "/api/v1/todos")
                .withContentType("application/json")
                .withBody(body);
        if (clientHeader) {
            builder.withHeader("X-Todo-Client", "web");
        }
        return builder.build();
    }
}
