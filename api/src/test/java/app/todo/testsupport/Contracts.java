package app.todo.testsupport;

import static org.assertj.core.api.Assertions.assertThat;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.report.MessageResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.schema.SchemaValidator;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
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

    private static final class TodosHolder {
        static final OpenApiInteractionValidator VALIDATOR =
                OpenApiInteractionValidator.createForSpecificationUrl(todosFile().toUri().toString()).build();
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
