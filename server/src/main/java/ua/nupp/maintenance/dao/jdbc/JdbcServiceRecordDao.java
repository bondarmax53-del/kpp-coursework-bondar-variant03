package ua.nupp.maintenance.dao.jdbc;

import ua.nupp.maintenance.dao.DaoException;
import ua.nupp.maintenance.dao.ServiceRecordDao;
import ua.nupp.maintenance.model.ServiceRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcServiceRecordDao implements ServiceRecordDao {
    private final ConnectionFactory cf;

    public JdbcServiceRecordDao(ConnectionFactory cf) {
        this.cf = cf;
    }

    private static final String COLUMNS =
            "id, request_id, specialist_id, work_description, hours_spent, cost, performed_at";

    @Override
    public List<ServiceRecord> findByRequest(long requestId) {
        String sql = "SELECT " + COLUMNS + " FROM service_record WHERE request_id = ? ORDER BY performed_at";
        try (Connection c = cf.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                List<ServiceRecord> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(map(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new DaoException("Помилка читання сервісних записів", e);
        }
    }

    @Override
    public Optional<ServiceRecord> findById(long id) {
        try (Connection c = cf.get();
             PreparedStatement ps = c.prepareStatement("SELECT " + COLUMNS + " FROM service_record WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DaoException("Помилка читання сервісного запису", e);
        }
    }

    @Override
    public boolean update(ServiceRecord r) {
        String sql = "UPDATE service_record SET specialist_id = ?, work_description = ?, hours_spent = ?, cost = ?,"
                + " performed_at = ? WHERE id = ?";
        try (Connection c = cf.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, r.getSpecialistId());
            ps.setString(2, r.getWorkDescription());
            ps.setBigDecimal(3, r.getHoursSpent());
            ps.setBigDecimal(4, r.getCost());
            ps.setTimestamp(5, JdbcSupport.toTimestamp(r.getPerformedAt()));
            ps.setLong(6, r.getId());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DaoException("Помилка оновлення сервісного запису", e);
        }
    }

    @Override
    public boolean delete(long id) {
        try (Connection c = cf.get(); PreparedStatement ps = c.prepareStatement("DELETE FROM service_record WHERE id = ?")) {
            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DaoException("Помилка видалення сервісного запису", e);
        }
    }

    private static ServiceRecord map(ResultSet rs) throws SQLException {
        ServiceRecord r = new ServiceRecord();
        r.setId(rs.getLong("id"));
        r.setRequestId(rs.getLong("request_id"));
        r.setSpecialistId(rs.getLong("specialist_id"));
        r.setWorkDescription(rs.getString("work_description"));
        r.setHoursSpent(rs.getBigDecimal("hours_spent"));
        r.setCost(rs.getBigDecimal("cost"));
        r.setPerformedAt(JdbcSupport.toLocal(rs.getTimestamp("performed_at")));
        return r;
    }

    @Override
    public ServiceRecord insert(ServiceRecord r) {
        String sql = "INSERT INTO service_record (request_id, specialist_id, work_description, hours_spent, cost, performed_at)"
                + " VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = cf.get();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, r.getRequestId());
            ps.setLong(2, r.getSpecialistId());
            ps.setString(3, r.getWorkDescription());
            ps.setBigDecimal(4, r.getHoursSpent());
            ps.setBigDecimal(5, r.getCost());
            ps.setTimestamp(6, JdbcSupport.toTimestamp(r.getPerformedAt()));
            ps.executeUpdate();
            r.setId(JdbcSupport.generatedId(ps));
            return r;
        } catch (SQLException e) {
            throw new DaoException("Помилка додавання сервісного запису", e);
        }
    }

    @Override
    public int countByRequest(long requestId) {
        try (Connection c = cf.get();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM service_record WHERE request_id = ?")) {
            ps.setLong(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DaoException("Помилка підрахунку сервісних записів", e);
        }
    }
}
