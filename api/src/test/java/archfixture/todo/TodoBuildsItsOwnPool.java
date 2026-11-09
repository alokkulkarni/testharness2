package archfixture.todo;

import com.zaxxer.hikari.HikariConfig;

/** Deliberate violation: only config may create a DataSource or a HikariConfig. */
public class TodoBuildsItsOwnPool {
    Object build() {
        return new HikariConfig();
    }
}
