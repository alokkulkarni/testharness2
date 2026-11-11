package app.todo.config;

import static org.assertj.core.api.Assertions.assertThat;

import app.todo.testsupport.Contracts;
import app.todo.testsupport.SpringContextTestBase;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Endpoint inventory (sdd.md section 14, ADR-0012): the union of the path-and-method pairs of
 * identity-api-v1.yaml and todo-api-v1.yaml equals the routes Spring MVC has mapped, excluding by name only
 * /error (all methods of ProblemErrorController); every pair is in exactly one file; and each operation's handler
 * is declared by a controller in that file's module package. Fails the build when the API grows an operation the
 * contract does not list (REQ-AUTH-005 AC-1) or serves an operation from the wrong module (NFR-007 AC-3).
 */
class EndpointInventoryTest extends SpringContextTestBase {

    private static final Set<String> VERBS = Set.of("get", "put", "post", "delete", "patch", "head", "options");
    private static final String ERROR_CONTROLLER = "app.todo.common.ProblemErrorController";

    @Autowired
    private ApplicationContext context;

    /** "GET /api/v1/todos/{}" -> operationId, for one contract file. */
    @SuppressWarnings("unchecked")
    private static Map<String, String> operationsOf(Path file) {
        Map<String, Object> paths = (Map<String, Object>) Contracts.yaml(file).get("paths");
        Map<String, String> operations = new TreeMap<>();
        for (Map.Entry<String, Object> path : paths.entrySet()) {
            for (Map.Entry<String, Object> item : ((Map<String, Object>) path.getValue()).entrySet()) {
                if (VERBS.contains(item.getKey())) {
                    String id = String.valueOf(((Map<String, Object>) item.getValue()).get("operationId"));
                    operations.put(key(item.getKey(), path.getKey()), id);
                }
            }
        }
        return operations;
    }

    private static String key(String method, String path) {
        return method.toUpperCase(Locale.ROOT) + " " + path.replaceAll("\\{[^}/]+}", "{}");
    }

    /** Spring's mapped routes (excluding ProblemErrorController), each with the package of its controller. */
    private Map<String, String> mappedRoutes() {
        Map<String, String> routes = new TreeMap<>();
        for (RequestMappingHandlerMapping mapping : context.getBeansOfType(RequestMappingHandlerMapping.class).values()) {
            for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : mapping.getHandlerMethods().entrySet()) {
                HandlerMethod handler = entry.getValue();
                if (handler.getBeanType().getName().equals(ERROR_CONTROLLER)) {
                    continue;
                }
                Set<RequestMethod> methods = entry.getKey().getMethodsCondition().getMethods();
                assertThat(methods).as("mapping %s must name its methods", entry.getKey()).isNotEmpty();
                Set<String> patterns = entry.getKey().getPathPatternsCondition().getPatternValues();
                for (String pattern : patterns) {
                    for (RequestMethod method : methods) {
                        routes.put(key(method.name(), pattern), handler.getBeanType().getPackageName());
                    }
                }
            }
        }
        return routes;
    }

    @Test
    @DisplayName("NFR-007 AC-3 the union of the two module contracts equals the routes Spring has mapped, excluding only /error")
    void NFR_007_AC3_contract_union_equals_the_mapped_routes() {
        Map<String, String> identity = operationsOf(Contracts.identityFile());
        Map<String, String> todos = operationsOf(Contracts.todosFile());
        Set<String> union = new TreeSet<>(identity.keySet());
        union.addAll(todos.keySet());

        assertThat(union).as("the contract files list 11 operations").hasSize(11);
        assertThat(mappedRoutes().keySet())
                .as("any mapped route missing from both module files is a spec question for the java-engineer")
                .containsExactlyInAnyOrderElementsOf(union);
    }

    @Test
    @DisplayName("NFR-007 AC-3 every path-and-method pair is in exactly one of the two contract files")
    void NFR_007_AC3_every_operation_is_in_exactly_one_contract_file() {
        Set<String> both = new TreeSet<>(operationsOf(Contracts.identityFile()).keySet());
        both.retainAll(operationsOf(Contracts.todosFile()).keySet());

        assertThat(both).isEmpty();
        Set<String> operationIds = new TreeSet<>(operationsOf(Contracts.identityFile()).values());
        operationIds.addAll(operationsOf(Contracts.todosFile()).values());
        assertThat(operationIds).containsExactlyInAnyOrder(
                "signUp", "logIn", "logOut", "getSession",
                "listTodos", "createTodo", "getTodo", "updateTodo", "setTodoDone", "deleteTodo", "clearCompletedTodos");
    }

    @Test
    @DisplayName("NFR-007 AC-3 each operation's handler is declared by a controller in its module package (identity: app.todo.auth, to-dos: app.todo.todo)")
    void NFR_007_AC3_each_operation_is_served_from_its_own_module_package() {
        Map<String, String> routes = mappedRoutes();

        for (String identityRoute : operationsOf(Contracts.identityFile()).keySet()) {
            assertThat(routes).containsKey(identityRoute);
            assertThat(routes.get(identityRoute)).as(identityRoute).matches("app\\.todo\\.auth(\\..*)?");
        }
        for (String todoRoute : operationsOf(Contracts.todosFile()).keySet()) {
            assertThat(routes).containsKey(todoRoute);
            assertThat(routes.get(todoRoute)).as(todoRoute).matches("app\\.todo\\.todo(\\..*)?");
        }
    }

    @Test
    @DisplayName("REQ-AUTH-005 AC-1 the contract has no user-listing or cross-user operation")
    void REQ_AUTH_005_AC1_contract_has_no_user_listing_or_cross_user_operation() {
        Set<String> all = new TreeSet<>(operationsOf(Contracts.identityFile()).keySet());
        all.addAll(operationsOf(Contracts.todosFile()).keySet());

        assertThat(all).noneMatch(route -> route.contains("/users") || route.contains("/admin"));
        assertThat(all).filteredOn(route -> route.startsWith("GET ")).containsExactlyInAnyOrder(
                "GET /api/v1/auth/session", "GET /api/v1/todos", "GET /api/v1/todos/{}");
    }
}
