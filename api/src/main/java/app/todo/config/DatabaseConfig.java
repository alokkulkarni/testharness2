package app.todo.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The only {@code DataSource} of the application (NFR-007 AC-1, ADR-0016): one HikariCP pool of exactly one
 * connection to the SQLite file named by {@code TODO_DB_PATH}, so database access is serialised in the application and
 * {@code SQLITE_BUSY} cannot occur between pool connections. Pragmas on the connection: WAL journal, foreign keys on,
 * 5000 ms busy timeout. The parent directory of the file is created on first start (REQ-DATA-001 AC-3); the schema is
 * created by the Flyway migration.
 */
@Configuration(proxyBeanMethods = false)
public class DatabaseConfig {

    @Bean(destroyMethod = "close")
    HikariDataSource dataSource(@Value("${todo.db.path:./data/todo.db}") String databasePath) throws IOException {
        Path file = Path.of(databasePath).toAbsolutePath();
        Path directory = file.getParent();
        if (directory != null) {
            Files.createDirectories(directory);
        }

        HikariConfig config = new HikariConfig();
        config.setPoolName("todo-sqlite");
        config.setJdbcUrl("jdbc:sqlite:" + file);
        config.setMaximumPoolSize(1);
        config.setMinimumIdle(1);
        config.addDataSourceProperty("journal_mode", "WAL");
        config.addDataSourceProperty("foreign_keys", "true");
        config.addDataSourceProperty("busy_timeout", "5000");
        return new HikariDataSource(config);
    }
}
