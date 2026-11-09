package archfixture.auth;

import java.sql.DriverManager;

/** Deliberate violation: only config may create a DataSource (any implementation) or open a connection itself. */
public class AuthOpensItsOwnConnection {
    Object build() {
        return new org.sqlite.SQLiteDataSource();
    }

    Object connect() throws Exception {
        return DriverManager.getConnection("jdbc:sqlite:fixture.db");
    }
}
