package ua.nupp.maintenance.dao.jdbc;

import ua.nupp.maintenance.dao.DaoException;
import ua.nupp.maintenance.dao.SpecialistDao;
import ua.nupp.maintenance.model.Specialist;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcSpecialistDao implements SpecialistDao {
    private static final String COLUMNS = "id, full_name, specialization, phone, email, active";

    private final ConnectionFactory cf;

    public JdbcSpecialistDao(ConnectionFactory cf) {
        this.cf = cf;
    }

    @Override
    public List<Specialist> findAll() {
        try (Connection c = cf.get();
             PreparedStatement ps = c.prepareStatement("SELECT " + COLUMNS + " FROM specialist ORDER BY full_name");
             ResultSet rs = ps.executeQuery()) {
            List<Specialist> list = new ArrayList<>();
            while (rs.next()) {
                list.add(map(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DaoException("Помилка читання фахівців", e);
        }
    }

    @Override
    public Optional<Specialist> findById(long id) {
        try (Connection c = cf.get();
             PreparedStatement ps = c.prepareStatement("SELECT " + COLUMNS + " FROM specialist WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DaoException("Помилка читання фахівця", e);
        }
    }

    @Override
    public Specialist insert(Specialist s) {
        String sql = "INSERT INTO specialist (full_name, specialization, phone, email, active) VALUES (?, ?, ?, ?, ?)";
        try (Connection c = cf.get();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            fill(ps, s);
            ps.executeUpdate();
            s.setId(JdbcSupport.generatedId(ps));
            return s;
        } catch (SQLException e) {
            throw new DaoException("Помилка додавання фахівця", e);
        }
    }

    @Override
    public boolean update(Specialist s) {
        String sql = "UPDATE specialist SET full_name = ?, specialization = ?, phone = ?, email = ?, active = ? WHERE id = ?";
        try (Connection c = cf.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            fill(ps, s);
            ps.setLong(6, s.getId());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DaoException("Помилка оновлення фахівця", e);
        }
    }

    @Override
    public boolean delete(long id) {
        try (Connection c = cf.get(); PreparedStatement ps = c.prepareStatement("DELETE FROM specialist WHERE id = ?")) {
            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DaoException("Помилка видалення фахівця", e);
        }
    }

    private static void fill(PreparedStatement ps, Specialist s) throws SQLException {
        ps.setString(1, s.getFullName());
        ps.setString(2, s.getSpecialization());
        ps.setString(3, s.getPhone());
        ps.setString(4, s.getEmail());
        ps.setBoolean(5, s.isActive());
    }

    private static Specialist map(ResultSet rs) throws SQLException {
        return new Specialist(
                rs.getLong("id"),
                rs.getString("full_name"),
                rs.getString("specialization"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getBoolean("active"));
    }
}
