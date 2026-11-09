package app.todo.config;

import static org.assertj.core.api.Assertions.assertThat;

import app.todo.testsupport.SpringContextTestBase;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

/**
 * Default-profile wiring: NFR-007 AC-1 (one application, one SQLite file, one connection pool), ADR-0016
 * (one connection, pragmas), ADR-0013 (the Clock bean is the system UTC clock; no test clock exists).
 * No test clock override is imported here, so this context is the application as `spring-boot:run` builds it.
 */
class ModuleWiringTest extends SpringContextTestBase {

    @Autowired
    private ApplicationContext context;

    @Test
    @DisplayName("NFR-007 AC-1 the application context holds exactly one DataSource: a HikariCP pool of at most one connection")
    void NFR_007_AC1_exactly_one_hikari_data_source_with_one_connection() {
        Map<String, DataSource> sources = context.getBeansOfType(DataSource.class);

        assertThat(sources).hasSize(1);
        DataSource only = sources.values().iterator().next();
        assertThat(only).isInstanceOf(HikariDataSource.class);
        assertThat(((HikariDataSource) only).getMaximumPoolSize()).isEqualTo(1);
    }

    @Test
    @DisplayName("NFR-007 AC-1 the pool's JDBC URL names the TODO_DB_PATH file")
    void NFR_007_AC1_jdbc_url_names_the_configured_file() {
        HikariDataSource pool = context.getBean(HikariDataSource.class);

        assertThat(pool.getJdbcUrl()).startsWith("jdbc:sqlite:").contains(dbFile().toString());
    }

    @Test
    @DisplayName("REQ-DATA-002 AC-2 the connection runs with WAL journal, foreign keys on and a 5000 ms busy timeout (ADR-0016)")
    void REQ_DATA_002_AC2_sqlite_pragmas_are_set_on_the_connection() throws Exception {
        DataSource ds = context.getBean(DataSource.class);

        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            assertThat(single(s, "PRAGMA journal_mode")).isEqualToIgnoringCase("wal");
            assertThat(single(s, "PRAGMA foreign_keys")).isEqualTo("1");
            assertThat(single(s, "PRAGMA busy_timeout")).isEqualTo("5000");
        }
    }

    @Test
    @DisplayName("REQ-DATA-001 AC-2 the schema is created by Flyway migration V1 in the SQLite file")
    void REQ_DATA_001_AC2_flyway_created_the_schema() throws Exception {
        DataSource ds = context.getBean(DataSource.class);

        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            assertThat(single(s, "SELECT count(*) FROM sqlite_master WHERE type='table' AND name IN ('users','sessions','todos','flyway_schema_history')"))
                    .isEqualTo("4");
            assertThat(single(s, "SELECT version FROM flyway_schema_history WHERE success = 1 ORDER BY installed_rank LIMIT 1"))
                    .isEqualTo("1");
        }
    }

    @Test
    @DisplayName("NFR-007 AC-1 todos.user_id is a foreign key to users that cascades on delete (accepted shared-schema coupling, ADR-0018)")
    void NFR_007_AC1_todos_user_id_is_a_cascading_foreign_key_to_users() throws Exception {
        DataSource ds = context.getBean(DataSource.class);

        try (Connection c = ds.getConnection(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery("PRAGMA foreign_key_list(todos)")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("table")).isEqualTo("users");
            assertThat(rs.getString("from")).isEqualTo("user_id");
            assertThat(rs.getString("on_delete")).isEqualToIgnoringCase("CASCADE");
        }
    }

    @Test
    @DisplayName("REQ-AUTH-004 AC-4 in the default profile the Clock bean is the system UTC clock and no test clock bean exists (ADR-0013)")
    void REQ_AUTH_004_AC4_default_profile_clock_is_the_system_utc_clock() {
        Map<String, Clock> clocks = context.getBeansOfType(Clock.class);

        assertThat(clocks).hasSize(1);
        Clock clock = clocks.values().iterator().next();
        assertThat(clock).isEqualTo(Clock.systemUTC());
        assertThat(clock.getZone()).isEqualTo(ZoneOffset.UTC);
        assertThat(context.getBeanDefinitionNames()).noneMatch(name -> {
            String lower = name.toLowerCase(java.util.Locale.ROOT);
            return lower.contains("offsetfileclock") || lower.contains("e2eclockconfiguration");
        });
    }

    @Test
    @DisplayName("REQ-AUTH-004 AC-4 the normal run has no way to move the clock: no clock-offset property is read by default")
    void REQ_AUTH_004_AC4_no_clock_offset_property_is_configured_by_default() {
        assertThat(environment.getProperty("todo.e2e.clock-offset-file")).isNull();
        assertThat(environment.getActiveProfiles()).doesNotContain("e2e");
    }

    private static String single(Statement s, String sql) throws Exception {
        try (ResultSet rs = s.executeQuery(sql)) {
            assertThat(rs.next()).as(sql).isTrue();
            return rs.getString(1);
        }
    }
}
