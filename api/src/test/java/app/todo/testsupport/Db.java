package app.todo.testsupport;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Read-only look into the application's SQLite file through a second, test-owned connection (the schema is the
 * one in sdd.md section 3). Used to check storage facts the API does not expose (REQ-AUTH-003, REQ-DATA-001).
 */
public final class Db {

    private final Path file;

    public Db(Path file) {
        this.file = file;
    }

    public Path file() {
        return file;
    }

    public long count(String sql, Object... params) {
        return queryOne(sql, params, rs -> rs.getLong(1));
    }

    public String text(String sql, Object... params) {
        return queryOne(sql, params, rs -> rs.getString(1));
    }

    /** Column names of a table, in table order. */
    public List<String> columns(String table) {
        List<String> names = new ArrayList<>();
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement("PRAGMA table_info(" + table + ")")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    names.add(rs.getString("name"));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        return names;
    }

    private interface Reader<T> {
        T read(ResultSet rs) throws SQLException;
    }

    private <T> T queryOne(String sql, Object[] params, Reader<T> reader) {
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? reader.read(rs) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("query failed: " + sql, e);
        }
    }

    private Connection open() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + file);
    }
}
