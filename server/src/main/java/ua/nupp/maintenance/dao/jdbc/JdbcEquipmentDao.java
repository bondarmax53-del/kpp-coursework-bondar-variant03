package ua.nupp.maintenance.dao.jdbc;

import ua.nupp.maintenance.dao.DaoException;
import ua.nupp.maintenance.dao.EquipmentDao;
import ua.nupp.maintenance.model.Equipment;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcEquipmentDao implements EquipmentDao {
    private static final String COLUMNS =
            "id, inventory_number, name, model, location, criticality, commissioned_date";

    private final ConnectionFactory cf;

    public JdbcEquipmentDao(ConnectionFactory cf) {
        this.cf = cf;
    }

    @Override
    public List<Equipment> findAll() {
        String sql = "SELECT " + COLUMNS + " FROM equipment ORDER BY name";
        try (Connection c = cf.get();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Equipment> list = new ArrayList<>();
            while (rs.next()) {
                list.add(map(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DaoException("Помилка читання обладнання", e);
        }
    }

    @Override
    public Optional<Equipment> findById(long id) {
        return findOne("SELECT " + COLUMNS + " FROM equipment WHERE id = ?", ps -> ps.setLong(1, id));
    }

    @Override
    public Optional<Equipment> findByInventoryNumber(String inventoryNumber) {
        return findOne("SELECT " + COLUMNS + " FROM equipment WHERE inventory_number = ?",
                ps -> ps.setString(1, inventoryNumber));
    }

    @Override
    public Equipment insert(Equipment e) {
        String sql = "INSERT INTO equipment (inventory_number, name, model, location, criticality, commissioned_date)"
                + " VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = cf.get();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            fill(ps, e);
            ps.executeUpdate();
            e.setId(JdbcSupport.generatedId(ps));
            return e;
        } catch (SQLException ex) {
            throw new DaoException("Помилка додавання обладнання", ex);
        }
    }

    @Override
    public boolean update(Equipment e) {
        String sql = "UPDATE equipment SET inventory_number = ?, name = ?, model = ?, location = ?,"
                + " criticality = ?, commissioned_date = ? WHERE id = ?";
        try (Connection c = cf.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            fill(ps, e);
            ps.setLong(7, e.getId());
            return ps.executeUpdate() == 1;
        } catch (SQLException ex) {
            throw new DaoException("Помилка оновлення обладнання", ex);
        }
    }

    @Override
    public boolean delete(long id) {
        try (Connection c = cf.get(); PreparedStatement ps = c.prepareStatement("DELETE FROM equipment WHERE id = ?")) {
            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException ex) {
            throw new DaoException("Помилка видалення обладнання", ex);
        }
    }

    // ---------- допоміжне ----------

    private interface Binder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    private Optional<Equipment> findOne(String sql, Binder binder) {
        try (Connection c = cf.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new DaoException("Помилка читання обладнання", ex);
        }
    }

    private static void fill(PreparedStatement ps, Equipment e) throws SQLException {
        ps.setString(1, e.getInventoryNumber());
        ps.setString(2, e.getName());
        ps.setString(3, e.getModel());
        ps.setString(4, e.getLocation());
        ps.setInt(5, e.getCriticality());
        ps.setDate(6, e.getCommissionedDate() == null ? null : Date.valueOf(e.getCommissionedDate()));
    }

    private static Equipment map(ResultSet rs) throws SQLException {
        Date d = rs.getDate("commissioned_date");
        return new Equipment(
                rs.getLong("id"),
                rs.getString("inventory_number"),
                rs.getString("name"),
                rs.getString("model"),
                rs.getString("location"),
                rs.getInt("criticality"),
                d == null ? null : d.toLocalDate());
    }
}
