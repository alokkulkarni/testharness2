package app.todo.architecture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.todo.architecture.ArchitectureRules.NamedRule;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * NFR-007 AC-2 non-vacuity (sdd.md section 14, risks): the same rules, built for a fixture root package outside
 * app.todo ({@code archfixture}, plain classes in the test source set with no Spring annotations), must each fail
 * on the fixture's one deliberate violation. A rule that matched no class would pass silently on the application;
 * this test shows each rule can fail, and for the reason it names.
 */
class ArchitectureNonVacuityTest {

    private static JavaClasses fixture;
    private static List<NamedRule> rules;

    /** For each rule: a fragment of the failure text that names the deliberate violation. */
    private static final Map<String, String> EXPECTED_VIOLATION = Map.of(
            "no cycles between auth, todo, common and config", "Cycle detected",
            "todo uses only todo, common and auth.api", "archfixture.todo.TodoPeeksIntoAuthInternals",
            "auth uses only auth and common", "archfixture.auth.AuthReachesIntoTodo",
            "common uses no module and not config", "archfixture.common.CommonReachesIntoAuth",
            "only config creates a DataSource or HikariConfig", "archfixture.todo.TodoBuildsItsOwnPool",
            "every main class lies in auth, auth.api, todo, common, config or the root package", "archfixture.stray.StrayClass",
            "auth.api publishes only CurrentUser", "archfixture.auth.api.NotTheSeam",
            "repositories are package-private in their module", "archfixture.auth.LeakyUserRepository");

    @BeforeAll
    static void importFixture() {
        fixture = new ClassFileImporter().importPackages("archfixture");
        rules = ArchitectureRules.forRoot("archfixture");
    }

    @Test
    @DisplayName("NFR-007 AC-2 the fixture has a class in every package the rules talk about")
    void NFR_007_AC2_fixture_covers_every_rule_subject() {
        for (String pkg : List.of("archfixture", "archfixture.auth", "archfixture.auth.api", "archfixture.todo",
                "archfixture.common", "archfixture.config", "archfixture.stray")) {
            assertThat(StreamSupport.stream(fixture.spliterator(), false).anyMatch(c -> c.getPackageName().equals(pkg)))
                    .as(pkg).isTrue();
        }
    }

    @Test
    @DisplayName("NFR-007 AC-2 every rule of the set has a named deliberate violation in the fixture")
    void NFR_007_AC2_every_rule_has_an_expected_violation() {
        assertThat(rules.stream().map(NamedRule::name)).containsExactlyInAnyOrderElementsOf(EXPECTED_VIOLATION.keySet());
    }

    @Test
    @DisplayName("NFR-007 AC-2 each rule fails on the fixture, naming its deliberate violation")
    void NFR_007_AC2_each_rule_fails_on_its_deliberate_violation() {
        for (NamedRule rule : rules) {
            assertThatThrownBy(() -> rule.rule().check(fixture))
                    .as(rule.name())
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining(EXPECTED_VIOLATION.get(rule.name()));
        }
    }

    @Test
    @DisplayName("NFR-007 AC-2 creating a DataSource implementation or opening a DriverManager connection outside config fails the rule; only using one does not")
    void NFR_007_AC2_creating_any_data_source_outside_config_is_a_violation_using_one_is_not() {
        NamedRule poolRule = rules.stream().filter(r -> r.name().startsWith("only config creates")).findFirst().orElseThrow();

        assertThatThrownBy(() -> poolRule.rule().check(fixture))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("archfixture.todo.TodoBuildsItsOwnPool")
                .hasMessageContaining("archfixture.auth.AuthOpensItsOwnConnection")
                .hasMessageContaining("SQLiteDataSource")
                .hasMessageContaining("DriverManager")
                .hasMessageNotContaining("archfixture.todo.TodoOnlyUsesADataSource");
    }

    @Test
    @DisplayName("NFR-007 AC-2 the published seam is allowed: a to-do class that uses only auth.api is not reported")
    void NFR_007_AC2_use_of_the_published_interface_is_not_a_violation() {
        NamedRule todoRule = rules.stream().filter(r -> r.name().startsWith("todo uses only")).findFirst().orElseThrow();

        assertThatThrownBy(() -> todoRule.rule().check(fixture))
                .isInstanceOf(AssertionError.class)
                .hasMessageNotContaining("archfixture.todo.TodoUsesPublishedSeam");
    }
}
