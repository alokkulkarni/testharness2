package archfixture.todo;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

/** Not a violation: using a DataSource that was handed in (never creating one) is allowed anywhere. */
public class TodoOnlyUsesADataSource {
    Connection borrow(DataSource given) throws SQLException {
        return given.getConnection();
    }
}
