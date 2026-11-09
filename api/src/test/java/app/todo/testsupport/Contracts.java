package app.todo.testsupport;

import static org.assertj.core.api.Assertions.assertThat;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.report.MessageResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.schema.SchemaValidator;
import com.atlassian.oai.validator.whitelist.ValidationErrorsWhitelist;
import com.atlassian.oai.validator.whitelist.rule.WhitelistRules;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.core.models.ParseOptions;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

/**
 * Reads the frozen contract files where they are frozen (docs/specs/CHG-0001/contracts/), never a copy
 * (ADR-0012). The repository root is found by walking up from the working directory, so the tests run from the
 * repository root ({@code mvn -f api/pom.xml}) and from {@code api/} alike.
 */
public final class Contracts {

    private static final String CONTRACTS = "docs/specs/CHG-0001/contracts";

    private Contracts() {}

    public static Path contractsDir() {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null) {
            Path candidate = dir.resolve(CONTRACTS);
            if (Files.isRegularFile(candidate.resolve("common-problem-v1.yaml"))) {
                return candidate;
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException("cannot find " + CONTRACTS + " above " + Path.of("").toAbsolutePath());
    }

    public static Path identityFile() {
        return contractsDir().resolve("identity-api-v1.yaml");
    }

    public static Path todosFile() {
        return contractsDir().resolve("todo-api-v1.yaml");
    }

    public static Path commonFile() {
        return contractsDir().resolve("common-problem-v1.yaml");
    }

    private static final class IdentityHolder {
        static final OpenApiInteractionValidator VALIDATOR =
                OpenApiInteractionValidator.createForSpecificationUrl(identityFile().toUri().toString()).build();
    }

    /**
     * The one request-validation override (REQ-LIST-002 AC-5 and AC-7; sdd.md section 7; the contract's own
     * description of {@code q}). The contract declares {@code q} on {@code GET /api/v1/todos} as
     * {@code required: false} and says an empty or whitespace-only {@code q} means "no search". The v3 request
     * validator nevertheless reports an empty or ASCII-space-only value as
     * {@code validation.request.parameter.missing} ("Parameter 'q' is required but is missing"), and
     * {@code allowEmptyValue: true} does not change that. This whitelist rule drops exactly that one message:
     * key {@code validation.request.parameter.missing}, method GET, path {@code /api/v1/todos}, text naming
     * parameter {@code q}. It cannot hide a missing required parameter of any other operation or parameter,
     * nor any other request or response violation (the response side is untouched). {@code q} is optional in
     * the contract, so a "missing" report for it is never a genuine violation.
     */
    private static final ValidationErrorsWhitelist EMPTY_SEARCH_IS_NO_SEARCH = ValidationErrorsWhitelist.create()
            .withRule(
                    "REQ-LIST-002 AC-5/AC-7: empty or blank q on GET /api/v1/todos is no search",
                    WhitelistRules.allOf(
                            WhitelistRules.messageHasKey("validation.request.parameter.missing"),
                            WhitelistRules.methodIs(PathItem.HttpMethod.GET),
                            WhitelistRules.pathContains("/api/v1/todos"),
                            WhitelistRules.messageContains("Parameter 'q' ")));

    private static final class TodosHolder {
        static final OpenApiInteractionValidator VALIDATOR =
                OpenApiInteractionValidator.createForSpecificationUrl(todosFile().toUri().toString())
                        .withWhitelist(EMPTY_SEARCH_IS_NO_SEARCH)
                        .build();
    }

    private static final class ProblemHolder {
        static final OpenAPI API;
        static final Schema<?> PROBLEM;
        static final SchemaValidator VALIDATOR;

        static {
            ParseOptions options = new ParseOptions();
            options.setResolve(true);
            API = new OpenAPIParser().readLocation(commonFile().toUri().toString(), null, options).getOpenAPI();
            PROBLEM = API.getComponents().getSchemas().get("Problem");
            VALIDATOR = new SchemaValidator(API, new MessageResolver());
        }
    }

    /** The validator built from the module file that defines the path (identity or to-dos). */
    public static OpenApiInteractionValidator validatorFor(String path) {
        return path.startsWith("/api/v1/auth/") ? IdentityHolder.VALIDATOR : TodosHolder.VALIDATOR;
    }

    /** Validates an error body directly against the {@code Problem} schema (ADR-0012: no operation exists). */
    public static void assertValidProblem(String json) {
        ValidationReport report = ProblemHolder.VALIDATOR.validate(json, ProblemHolder.PROBLEM, "response.body");
        assertThat(report.hasErrors()).as("Problem schema violations for %s: %s", json, report.getMessages()).isFalse();
    }

    /** Parses a contract file into maps and lists (SnakeYAML). */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> yaml(Path file) {
        try (Reader reader = Files.newBufferedReader(file)) {
            return (Map<String, Object>) new Yaml().load(reader);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
