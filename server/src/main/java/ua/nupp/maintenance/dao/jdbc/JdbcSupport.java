package ua.nupp.maintenance.dao.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;

/** Допоміжні методи для JDBC-DAO (робота з null і датами). */
final class JdbcSupport {
    private JdbcSupport() {
    }

    static Long getLongOrNull(ResultSet rs, String column) throws SQLException {
        long v = rs.getLong(column);
        return rs.wasNull() ? null : v;
    }

    static void setLongOrNull(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.BIGINT);
        } else {
            ps.setLong(index, value);
        }
    }

    static LocalDateTime toLocal(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }

    static Timestamp toTimestamp(LocalDateTime dt) {
        return dt == null ? null : Timestamp.valueOf(dt);
    }

    static long generatedId(PreparedStatement ps) throws SQLException {
        try (ResultSet keys = ps.getGeneratedKeys()) {
            if (keys.next()) {
                return keys.getLong(1);
            }
            throw new SQLException("БД не повернула згенерований id");
        }
    }
}
