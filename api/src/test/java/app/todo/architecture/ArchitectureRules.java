package app.todo.architecture;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideOutsideOfPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaCall;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.properties.HasOwner;
import com.tngtech.archunit.lang.ArchRule;
import java.util.List;

/**
 * The ArchUnit rule set of ADR-0018 (sdd.md section 14), built for a given root package so the same rules run on
 * the application ({@code app.todo}) and on a fixture root ({@code archfixture}) that violates each of them.
 */
final class ArchitectureRules {

    /** A named rule, so a failure and a parameterised test name say which boundary broke. */
    record NamedRule(String name, ArchRule rule) {
        @Override
        public String toString() {
            return name;
        }
    }

    private ArchitectureRules() {}

    static List<NamedRule> forRoot(String root) {
        String auth = root + ".auth";
        String todo = root + ".todo";
        String common = root + ".common";
        String config = root + ".config";

        // todo uses only todo, common and auth.api
        DescribedPredicate<JavaClass> forbiddenForTodo = resideInAnyPackage(auth + "..", config + "..")
                .and(resideOutsideOfPackage(auth + ".api.."))
                .or(resideInAPackage(root));
        // auth uses only auth and common
        DescribedPredicate<JavaClass> forbiddenForAuth = resideInAnyPackage(todo + "..", config + "..")
                .or(resideInAPackage(root));
        // common uses no module and not config
        DescribedPredicate<JavaClass> forbiddenForCommon = resideInAnyPackage(auth + "..", todo + "..", config + "..")
                .or(resideInAPackage(root));
        // creating a DataSource or a HikariConfig: a constructor call on any DataSource implementation or on
        // HikariConfig, DataSourceBuilder's static factory, or opening a connection through DriverManager.
        // Merely using the type (a field, a parameter, JdbcClient) is not creating one.
        DescribedPredicate<JavaCall<?>> constructsDataSource = JavaCall.Predicates.target(HasOwner.Predicates.With.owner(
                JavaClass.Predicates.assignableTo("javax.sql.DataSource")
                        .or(JavaClass.Predicates.assignableTo("com.zaxxer.hikari.HikariConfig"))));
        DescribedPredicate<JavaCall<?>> buildsOrConnects = JavaCall.Predicates.target(HasOwner.Predicates.With.owner(
                JavaClass.Predicates.simpleName("DataSourceBuilder")
                        .or(JavaClass.Predicates.assignableTo("java.sql.DriverManager"))));

        return List.of(
                new NamedRule("no cycles between auth, todo, common and config",
                        slices().matching(root + ".(*)..").should().beFreeOfCycles()),
                new NamedRule("todo uses only todo, common and auth.api",
                        noClasses().that().resideInAPackage(todo + "..").should().dependOnClassesThat(forbiddenForTodo)),
                new NamedRule("auth uses only auth and common",
                        noClasses().that().resideInAPackage(auth + "..").should().dependOnClassesThat(forbiddenForAuth)),
                new NamedRule("common uses no module and not config",
                        noClasses().that().resideInAPackage(common + "..").should().dependOnClassesThat(forbiddenForCommon)),
                new NamedRule("only config creates a DataSource or HikariConfig",
                        noClasses().that().resideOutsideOfPackage(config + "..")
                                .should().callConstructorWhere(constructsDataSource)
                                .orShould().callMethodWhere(buildsOrConnects)),
                new NamedRule("every main class lies in auth, auth.api, todo, common, config or the root package",
                        classes().should().resideInAnyPackage(root, auth + "..", todo + "..", common + "..", config + "..")),
                new NamedRule("auth.api publishes only CurrentUser",
                        classes().that().resideInAPackage(auth + ".api..").should().haveSimpleName("CurrentUser")),
                new NamedRule("repositories are package-private in their module",
                        classes().that().haveSimpleNameEndingWith("Repository")
                                .and().resideInAnyPackage(auth + "..", todo + "..")
                                .should().notBePublic()));
    }
}
