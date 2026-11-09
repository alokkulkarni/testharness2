package app.todo.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import app.todo.architecture.ArchitectureRules.NamedRule;
import app.todo.auth.api.CurrentUser;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * NFR-007 AC-2: a test fails if code in one module uses a type another module does not publish, or if module
 * dependencies form a cycle. ArchUnit over the API's main classes only (test classes, including
 * app.todo.TestTodoApplication and app.todo.testsupport.*, are not imported). ADR-0018, sdd.md section 14.
 */
class ArchitectureTest {

    private static JavaClasses mainClasses;

    @BeforeAll
    static void importMainClasses() {
        mainClasses = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("app.todo");
    }

    static Stream<NamedRule> applicationRules() {
        return ArchitectureRules.forRoot("app.todo").stream();
    }

    @Test
    @DisplayName("NFR-007 AC-2 the main classes of the application are found (the rules are not checked against nothing)")
    void NFR_007_AC2_main_classes_are_imported() {
        assertThat(mainClasses.contain("app.todo.auth.api.CurrentUser")).isTrue();
        for (String pkg : new String[] {"app.todo.auth", "app.todo.todo", "app.todo.common", "app.todo.config"}) {
            assertThat(StreamSupport.stream(mainClasses.spliterator(), false).filter(c -> c.getPackageName().startsWith(pkg)).count())
                    .as(pkg).isPositive();
        }
        assertThat(StreamSupport.stream(mainClasses.spliterator(), false).map(c -> c.getName()))
                .as("test-only classes are not main classes")
                .noneMatch(n -> n.startsWith("app.todo.testsupport.") || n.equals("app.todo.TestTodoApplication"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("applicationRules")
    @DisplayName("NFR-007 AC-2 the module boundary rule holds for the application's main classes")
    void NFR_007_AC2_module_boundary_rule_holds(NamedRule rule) {
        rule.rule().check(mainClasses);
    }

    @Test
    @DisplayName("NFR-007 AC-2 the one type the identity module publishes is CurrentUser(long userId)")
    void NFR_007_AC2_the_published_seam_is_current_user_with_a_long_user_id() {
        assertThat(CurrentUser.class.isRecord()).isTrue();
        assertThat(Modifier.isPublic(CurrentUser.class.getModifiers())).isTrue();
        RecordComponent[] components = CurrentUser.class.getRecordComponents();
        assertThat(components).hasSize(1);
        assertThat(components[0].getName()).isEqualTo("userId");
        assertThat(components[0].getType()).isEqualTo(long.class);
        assertThat(CurrentUser.class.getPackageName()).isEqualTo("app.todo.auth.api");
    }
}
