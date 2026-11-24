package ua.nupp.maintenance.dao.jdbc;

import ua.nupp.maintenance.dao.DaoException;
import ua.nupp.maintenance.dao.MaintenanceRequestDao;
import ua.nupp.maintenance.model.MaintenanceRequest;
import ua.nupp.maintenance.model.RequestStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcMaintenanceRequestDao implements MaintenanceRequestDao {
    private static final String COLUMNS = "id, equipment_id, specialist_id, description, severity,"
            + " downtime_hours, diagnosis, status, priority, created_at, updated_at";

    private final ConnectionFactory cf;

    public JdbcMaintenanceRequestDao(ConnectionFactory cf) {
        this.cf = cf;
    }

    @Override
    public List<MaintenanceRequest> findAll(RequestStatus statusOrNull) {
        String sql = "SELECT " + COLUMNS + " FROM maintenance_request"
                + (statusOrNull == null ? "" : " WHERE status = ?")
                + " ORDER BY priority DESC, created_at";
        try (Connection c = cf.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (statusOrNull != null) {
                ps.setString(1, statusOrNull.name());
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<MaintenanceRequest> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(map(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new DaoException("Помилка читання заявок", e);
        }
    }

    @Override
    public Optional<MaintenanceRequest> findById(long id) {
        try (Connection c = cf.get();
             PreparedStatement ps = c.prepareStatement("SELECT " + COLUMNS + " FROM maintenance_request WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DaoException("Помилка читання заявки", e);
        }
    }

    @Override
    public MaintenanceRequest insert(MaintenanceRequest r) {
        String sql = "INSERT INTO maintenance_request (equipment_id, specialist_id, description, severity,"
                + " downtime_hours, diagnosis, status, priority, created_at, updated_at)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = cf.get();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            fill(ps, r);
            ps.executeUpdate();
            r.setId(JdbcSupport.generatedId(ps));
            return r;
        } catch (SQLException e) {
            throw new DaoException("Помилка додавання заявки", e);
        }
    }

    @Override
    public boolean update(MaintenanceRequest r) {
        String sql = "UPDATE maintenance_request SET equipment_id = ?, specialist_id = ?, description = ?,"
                + " severity = ?, downtime_hours = ?, diagnosis = ?, status = ?, priority = ?,"
                + " created_at = ?, updated_at = ? WHERE id = ?";
        try (Connection c = cf.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            fill(ps, r);
            ps.setLong(11, r.getId());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DaoException("Помилка оновлення заявки", e);
        }
    }

    @Override
    public boolean delete(long id) {
        // сервісні записи видаляються каскадно (ON DELETE CASCADE)
        try (Connection c = cf.get(); PreparedStatement ps = c.prepareStatement("DELETE FROM maintenance_request WHERE id = ?")) {
            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DaoException("Помилка видалення заявки", e);
        }
    }

    @Override
    public int countByEquipment(long equipmentId) {
        return count("SELECT COUNT(*) FROM maintenance_request WHERE equipment_id = ?", equipmentId);
    }

    @Override
    public int countBySpecialist(long specialistId) {
        return count("SELECT COUNT(*) FROM maintenance_request WHERE specialist_id = ?", specialistId);
    }

    private int count(String sql, long id) {
        try (Connection c = cf.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DaoException("Помилка підрахунку заявок", e);
        }
    }

    private static void fill(PreparedStatement ps, MaintenanceRequest r) throws SQLException {
        ps.setLong(1, r.getEquipmentId());
        JdbcSupport.setLongOrNull(ps, 2, r.getSpecialistId());
        ps.setString(3, r.getDescription());
        ps.setInt(4, r.getSeverity());
        ps.setInt(5, r.getDowntimeHours());
        ps.setString(6, r.getDiagnosis());
        ps.setString(7, r.getStatus().name());
        ps.setDouble(8, r.getPriority());
        ps.setTimestamp(9, JdbcSupport.toTimestamp(r.getCreatedAt()));
        ps.setTimestamp(10, JdbcSupport.toTimestamp(r.getUpdatedAt()));
    }

    private static MaintenanceRequest map(ResultSet rs) throws SQLException {
        MaintenanceRequest r = new MaintenanceRequest();
        r.setId(rs.getLong("id"));
        r.setEquipmentId(rs.getLong("equipment_id"));
        r.setSpecialistId(JdbcSupport.getLongOrNull(rs, "specialist_id"));
        r.setDescription(rs.getString("description"));
        r.setSeverity(rs.getInt("severity"));
        r.setDowntimeHours(rs.getInt("downtime_hours"));
        r.setDiagnosis(rs.getString("diagnosis"));
        r.setStatus(RequestStatus.valueOf(rs.getString("status")));
        r.setPriority(rs.getDouble("priority"));
        r.setCreatedAt(JdbcSupport.toLocal(rs.getTimestamp("created_at")));
        r.setUpdatedAt(JdbcSupport.toLocal(rs.getTimestamp("updated_at")));
        return r;
    }
}
